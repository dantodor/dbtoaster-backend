package ddbt.codegen

/**
 * Transform a typed AST into vanilla C++ code (String).
 *
 * @author Mohammad Dashti, Milos Nikolic
 */

object CppGen {
  
  val VALUE_NAME = "__av"

  val EXPERIMENTAL_RUNTIME_LIBRARY = false
  val EXPERIMENTAL_HASHMAP = true
  val EXPERIMENTAL_MAX_INDEX_VARS = Int.MaxValue  
}

class CppGen(val cgOpts: CodeGenOptions) extends ICppGen

trait ICppGen extends CodeGen {

  import scala.collection.mutable.{ HashMap, HashSet, ArrayBuffer }
  import ddbt.lib.Utils._
  import ddbt.lib.TypeHelper.Cpp._
  import ddbt.ast._
  import ddbt.ast.M3._ 
  import CppGen._

  private var deltaRelationNames = Set[String]()
  
  /**
   * By default, each user-provided (top-level) query is materialized as a single map.
   * If this flag is turned on, the compiler will materialize top-level queries as multiple maps (if it is more efficient to do so),
   * and only combine them on request. For more complex queries (in particular nested aggregate, and AVG aggregate queries),
   * this results in faster processing rates, and if fresh results are required less than once per update, a lower overall computational cost as well.
   * However, because the final evaluation of the top-level query is not performed until a result is requested, access latencies are higher.
   * This optimization is not activated by default at any optimization level.
   */
  private var isExpressiveTLQSEnabled = false

  private var isBatchModeActive = false
  
  private def getIndexId(m: String, is: List[Int]): String = 
    (if (is.isEmpty) (0 until mapDefs(m).keys.size).toList else is).mkString //slice(m,is)


  protected def mapTypeToString(m: MapDef) = 
    if (m.keys.size == 0) typeToString(m.tp) else m.name + "_map"

  protected def queryTypeToString(q: Query) = 
    if (q.expr.ovars.size == 0) typeToString(q.expr.tp) else q.name + "_map"

  protected def queryRefTypeToString(q: Query) = 
    if (q.expr.ovars.size == 0) typeToString(q.expr.tp) else q.name + "_map&"

  protected def cmpToString(op: OpCmp) = op match {
    case OpEq => "=="
    case OpNe => "!="
    case OpGt => ">"
    case OpGe => ">="
  }

  //---------- Slicing (secondary) indices for a map
  protected val secondaryIndices = 
    HashMap[String, List[List[Int]]]().withDefaultValue(List[List[Int]]())
  
  private def registerSecondaryIndex(mapName: String, columns: List[Int]): Int = {
    val indices = secondaryIndices(mapName)
    val pos = indices.indexOf(columns)
    if (pos != -1) pos 
    else {
      secondaryIndices += (mapName -> (indices ::: List(columns)))
      indices.size
    }
  }
  
  // ---------- Methods manipulating with temporary maps

  // tmp mapName => (List of key types and value type)
  private val tempMapDefinitions = HashMap[String, (List[Type], Type)]()

  // Initialize and (if needed) register a temp variable
  private def initializeTempMap(n: String, tp: Type, ksTp: List[Type] = Nil): String =     
    if (ksTp == Nil) {
      typeToString(tp) + " " + n + " = " + zeroOfType(tp) + ";\n"
    }
    else {
      tempMapDefinitions += (n -> (ksTp, tp))
      n + ".clear();\n"
    }

  protected def emitTempMapDefinitions = {
    val s = tempMapDefinitions.map { case (n, (ksTp, vTp)) =>
      if (EXPERIMENTAL_HASHMAP)
          "MultiHashMap<" + tempEntryTypeName(ksTp, vTp) + ", " + typeToString(vTp) + 
            ", PrimaryHashIndex<" + tempEntryTypeName(ksTp, vTp) + "> > " + n + ";"
      else 
          "MultiHashMap<" + tempEntryTypeName(ksTp, vTp) + ", " + typeToString(vTp) + 
            ", HashIndex<" + tempEntryTypeName(ksTp, vTp) + ", " + typeToString(vTp) + "> > " + n + ";"
    }.mkString("\n")

    stringIf(s.nonEmpty, "/* Data structures used as temporary materialized views */\n" + s)
  }

  private val tempEntryTypes = HashSet[(List[Type], Type)]()

  protected def emitTempEntryTypes = {
    val s = tempEntryTypes.map { case (ksTp, vTp) =>
      val name = tempEntryTypeName(ksTp, vTp)
      val ksTpIdx = ksTp.zipWithIndex
      val sKeyDefs = ksTpIdx.map { case (t, i) => typeToString(t) + " _" + (i + 1) + ";" }.mkString(" ")
      val sValueDef = typeToString(vTp) + " " + VALUE_NAME + ";"
      val sModFnParams = ksTpIdx.map { case (t, i) => "const " + refTypeToString(t) + " c" + (i + 1) }.mkString(", ")
      val sModFnBody = ksTpIdx.map { case (_, i) => "_" + (i + 1) + " = c" + (i + 1) + ";"}.mkString(" ")
      val sEqualFnBody = ksTpIdx.map { case (_, i) => "(x._" + (i + 1) + " == y._" + (i + 1) + ")" }.mkString(" && ")
      val sHashFnBody = ksTpIdx.take(EXPERIMENTAL_MAX_INDEX_VARS)
                               .map { case (_, i) => "hash_combine(h, e._" + (i + 1) + ");" }.mkString("\n")

      s"""|struct ${name} {
          |  ${sKeyDefs} ${sValueDef}; ${name}* nxt; ${name}* prv;
          |  explicit ${name}() : nxt(nullptr), prv(nullptr) { }
          |  FORCE_INLINE ${name}& modify(${sModFnParams}) { ${sModFnBody} return *this; }
          |  static bool equals(const ${name} &x, const ${name} &y) {
          |    return (${sEqualFnBody});
          |  }
          |  static long hash(const ${name} &e) {
          |    size_t h = 0;
          |${ind(sHashFnBody, 2)}
          |    return h;
          |  }
          |};
          |""".stripMargin
    }.mkString("\n")

    stringIf(s.nonEmpty, "/* Temporary entry type definitions */\n" + s)
  }

  private def tempEntryTypeName(ksTp: List[Type], vTp: Type): String = 
    "tuple" + (ksTp.size + 1) + "_" + ksTp.map(typeToChar).mkString + "_" + typeToChar(vTp)


  // ---------- Local entry definitions (name and type) are accumulated in this variable
  
  private val localEntries = ArrayBuffer[(String, String)]()

  protected def emitLocalEntryDefinitions = {
    val s = localEntries.map { case (n, t) => t + " " + n + ";" }.mkString("\n")
    stringIf(s.nonEmpty, "/* Preallocated map entries (to avoid recreation of temporary objects) */\n" + s)
  }

  // ---------- Regular expression methods

  private def emitRegexInit = {
    val s = regexpCacheMap.map { case (regex, rvar) =>
        s"""|if (regcomp(&${rvar}, "${regex}", REG_EXTENDED | REG_NOSUB)) {
            |  cerr << "Error compiling regular expression: /${regex}/" << endl;
            |  exit(-1);
            |}
            |""".stripMargin
      }.mkString("\n")

    stringIf(s.nonEmpty, "/* Regular expression initialization */\n" + s)
  }

  private def emitRegexDestroy = {
    val s = regexpCacheMap.map { "regfree(&" + _._2 + ");" }.mkString("\n")
    stringIf(s.nonEmpty,  s"""|~data_t() {
                              |${ind(s)}
                              |}
                              |""".stripMargin)
  }

  private def emitRegexDefinitions = {
    val s = regexpCacheMap.map { "regex_t " + _._2 + ";" }.mkString("\n")
    stringIf(s.nonEmpty, "/* Regular expression objects */\n" + s)
  }

  // ---------- Methods manipulating with constants

  // Constant member definition
  private def emitConstDefinitions = {
    val s = hoistedConsts.map { case (a, n) =>
      "/* const static */ " + typeToString(a.tp) + " " + n + ";"
    }.mkString("\n")

    stringIf(s.nonEmpty, "/* Constant defitions */\n" + s)
  }

  // Constant member initilization
  private def emitConstInits = 
    hoistedConsts.map { case (Apply(fn, _, args), n) => 
      if (fn == "STRING_TYPE") {      // string initilization
        assert(args.size == 1)        // sanity check
        n + " = " + fn + "(\"" + args(0).asInstanceOf[Const].v + "\");"
      }
      else {
        val sArgs = args.map {
          case Const(TypeString, v) => "STRING_TYPE(\"" + v + "\")"
          case a => cpsExpr(a)
        }.mkString(", ")
        n + " = U" + fn + "(" + sArgs + ");"
      }
    }.mkString("\n")

  // ----------

  protected def emitMapDefinitions(maps: List[MapDef], queries: List[Query]) = {
    val s = maps.filter { m => !queries.exists(_.name == m.name) }   // exclude TLQ maps
                 .map { m => mapTypeToString(m) + " " + m.name + ";" }
                 .mkString("\n")
    stringIf(s.nonEmpty, "/* Data structures used for storing materialized views */\n" + s)
  }

  private def emitDataDefinitions(maps: List[MapDef], queries: List[Query]) = {    
    s"""|
        |${ind(emitLocalEntryDefinitions)}
        |
        |${ind(emitRegexDefinitions)}
        |
        |${ind(emitMapDefinitions(maps, queries))}
        |
        |${ind(emitTempMapDefinitions)}
        |
        |${ind(emitConstDefinitions)}
        |""".stripMargin
  } 

  // ------- Trigger generation (Start)
  private def emitTriggerStmt(s: TriggerStmt): String = {
      val localVar = fresh("se")

      if (s.target.keys.size > 0) {
        localEntries += ((localVar, s.target.name + "_entry"))
      }

      val sResetTargetMap = s.op match {
        case OpSet if s.target.keys.size > 0 => s"${s.target.name}.clear();\n"
        case _ => ""
      }

      val sInitExpr = s.initExpr.map { iexpr =>
        ctx.load()
        cpsExpr(iexpr, (v: String) =>
          if (s.target.keys.size == 0) 
            s"if (${s.target.name} == ${zeroOfType(iexpr.tp)}) ${s.target.name} = ${v};\n"
          else {
            val sArgs = localVar + ".modify(" + (s.target.keys map (x => rn(x._1))).mkString(", ") + ")"
            s"if (${s.target.name}.getValueOrDefault(${sArgs}) == ${zeroOfType(iexpr.tp)}) ${s.target.name}.setOrDelOnZero(${localVar}, ${v});\n"
          }
        ) 
      }.getOrElse("")

      val sStatement = {
        val (fop, sop) = s.op match { 
          case OpAdd => (s.target.name + ".addOrDelOnZero", "+=") 
          case OpSet => (s.target.name + ".addOrDelOnZero", "=") 
        }

        ctx.load()        
        cpsExpr(s.expr, (v: String) => {
            if (s.target.keys.size == 0) {
              extractBooleanExp(v) match {
                case Some((c, t)) =>
                  s"(/*if */(${c}) ? ${s.target.name} ${sop} ${t} : ${zeroOfType(s.target.tp)});\n"
                case _ =>
                  s"${s.target.name} ${sop} ${v};\n"
              }
            }
            else {
              val argList = (s.target.keys map (x => rn(x._1))).mkString(", ")
              extractBooleanExp(v) match {
                case Some((c,t)) =>            
                  s"(/*if */(${c}) ? ${fop}(${localVar}.modify(${argList}), ${t}) : (void)0);\n"
                case _ =>
                  s"${fop}(${localVar}.modify(${argList}), ${v});\n"
              }
            }
          },
          /*if (op==OpAdd)*/ Some(s.target.keys) /*else None*/
        )
      }

      sResetTargetMap + sInitExpr + sStatement
  }

  private def emitTrigger(t: Trigger): String = {
    // Generate trigger statements    
    val sTriggerBody = {
      ctx = Ctx(t.event.params.map { case (n, t) => (n, (t, n)) }.toMap)
      val body = t.stmts.map(emitTriggerStmt).mkString("\n")
      ctx = null
      body 
    }
  
    def emitBatchTrigger(s: Schema, body: String) = {
      val sTimeout = stringIf(cgOpts.timeoutMilli > 0, 
        s"""|if (tS > 0) { tS += batchSize; return; }
            |gettimeofday(&t, NULL);
            |tT = (t.tv_sec - t0.tv_sec) * 1000L;
            |if (tT > ${cgOpts.timeoutMilli}) { tS = batchSize; return; }
            |""".stripMargin)
        if (EXPERIMENTAL_RUNTIME_LIBRARY) {
        s"""|void on_batch_update_${s.name}(const std::vector<${s.name}_entry>::iterator &begin, const std::vector<${s.name}_entry>::iterator &end) {
            |  long batchSize = std::distance(begin, end);
            |${ind(sTimeout)}
            |  tN += batchSize;
            |${ind(body)}
            |}
            |""".stripMargin
      }
      else {
        s"""|void on_batch_update_${s.name}(${delta(s.name)}_map &${delta(s.name)}) {
            |  long batchSize = ${delta(s.name)}.count();
            |${ind(sTimeout)}
            |  tN += batchSize;
            |${ind(body)}
            |}
            |""".stripMargin
      }      
    }

    def emitSingleTupleTrigger(s: Schema, prefix: String, body: String) = {
      val sTimeout = stringIf(cgOpts.timeoutMilli > 0, 
        s"""|if (tS > 0) { ++tS; return; }
            |if ((tN & 127) == 0) {
            |  gettimeofday(&t, NULL);
            |  tT = (t.tv_sec - t0.tv_sec) * 1000L;
            |  if (tT > ${cgOpts.timeoutMilli}) { tS = 1; return; }
            |}
            |""".stripMargin)
      val sName = prefix + s.name
      val sParams = s.fields.map { case (n, t) => s"const ${refTypeToString(t)} ${n}" }.mkString(", ")

      s"""|void ${sName}(${sParams}) {
          |${ind(sTimeout)}
          |  ++tN;
          |${ind(body)}
          |}
          |""".stripMargin +
      // // TODO: Perhaps this could be kept together with struct definitions for each relation
      stringIf(EXPERIMENTAL_RUNTIME_LIBRARY, {
        val sArgs = s.fields.map { case (n, _) => s"e.${n}" }.mkString(", ")
        s"""|void ${sName}(${s.name}_entry &e) {
            |  ${sName}(${sArgs});
            |}
            |""".stripMargin
      })
    }

    def emitReadyTrigger(body: String) = {
      s"""|void on_system_ready_event() {
          |${ind(body)}
          |}
          |""".stripMargin      
    }

    t.event match {
      case EventBatchUpdate(s) => emitBatchTrigger(s, sTriggerBody)
      case EventInsert(s) => emitSingleTupleTrigger(s, "on_insert_", sTriggerBody)
      case EventDelete(s) => emitSingleTupleTrigger(s, "on_delete_", sTriggerBody)
      case EventReady => emitReadyTrigger(sTriggerBody)
    }
  } 

  protected def emitFieldNames(fields: List[(String, Type)]) = fields.map(_._1).mkString(", ")

  protected def emitInsertToMapFunc(map: String, entry: String) = 
    map + ".addOrDelOnZero(" + entry + ", 1L);"

  protected def emitTableTriggers(sources: List[Source]) = {
    sources.filter(!_.isStream).map { s =>
      val name = s.schema.name
      val fields = s.schema.fields
      val params = fields.map { case (n, t) => "const " + typeToString(t) + " " + n }.mkString(", ")
      val args = emitFieldNames(fields)

      s"""|void on_insert_${name}(${params}) {
          |  ${name}_entry e(${args}, 1L);
          |  ${emitInsertToMapFunc(name, "e")}
          |}
          |""".stripMargin +
      // 
      // TODO: perhaps enable in all cases
      (if (EXPERIMENTAL_RUNTIME_LIBRARY) {
         s"""|void on_insert_${name}(${name}_entry &e) {
             |  e.${VALUE_NAME} = 1L;
             |  ${name}.addOrDelOnZero(e, 1L);
             |}
             |""".stripMargin
      }
      else {
        val castArgs = fields.zipWithIndex.map { case ((_, tp), i) => 
            "*(reinterpret_cast<" + typeToString(tp) + "*>((*ea)[" + i + "].get()))"
          }.mkString(", ")

        emitUnwrapFunction(EventInsert(s.schema), sources) +
        // 
        stringIf(isBatchModeActive, 
          s"""|void unwrap_batch_update_${name}(const event_args_t& eaList) {
              |  size_t sz = eaList.size();
              |  for (size_t i = 0; i < sz; i++) {
              |    event_args_t* ea = reinterpret_cast<event_args_t*>(eaList[i].get());
              |    ${name}_entry e(${castArgs}, 1L);
              |    ${emitInsertToMapFunc(name, "e")}
              |  }
              |}
              |""".stripMargin)
      })
    }.mkString("\n")
  }
  
  protected def emitStreamTriggers(triggers: List[Trigger], sources: List[Source]) = {
    triggers.map { t =>
      emitTrigger(t) + stringIf(t.event != EventReady, "\n" + emitUnwrapFunction(t.event, sources))
    }.mkString("\n")
  }

  private def emitTriggerFunctions(s0: System) = {
    s"""|/* Trigger functions for table relations */
        |${emitTableTriggers(s0.sources)}
        |
        |/* Trigger functions for stream relations */
        |${emitStreamTriggers(s0.triggers, s0.sources)}
        |""".stripMargin
  }

  protected def emitUnwrapFunction(evt: EventTrigger, sources: List[Source]) = stringIf(!CppGen.EXPERIMENTAL_RUNTIME_LIBRARY, {
    val (op, name, fields) = evt match {
      case EventBatchUpdate(Schema(n, cs)) => ("batch_update", n, cs)
      case EventInsert(Schema(n, cs)) => ("insert", n, cs)
      case EventDelete(Schema(n, cs)) => ("delete", n, cs)
      case _ => sys.error("Unsupported trigger event " + evt)
    }
    val constructorArgs = fields.zipWithIndex.map { case ((_, t), i) => 
      "*(reinterpret_cast<" + typeToString(t) + "*>(ea[" + i + "].get()))"
    }.mkString(", ")

    evt match {
      case EventBatchUpdate(_) =>
        var code =    "void unwrap_" + op + "_" + name + "(const event_args_t& eaList) {\n"
        code = code + "  size_t sz = eaList.size();\n"
        for (s <- sources.filter(_.isStream)) {
          code = code + "  " + delta(s.schema.name) + ".clear();\n"
        }       
        code = code +   "  for (size_t i = 0; i < sz; i++) {\n"
        code = code +   "    event_args_t* ea = reinterpret_cast<event_args_t*>(eaList[i].get());\n"
        code = code +   "    relation_id_t relation = *(reinterpret_cast<relation_id_t*>((*ea).back().get()));\n"
        
        for (s <- sources.filter(_.isStream)) {
          val schema = s.schema
          val constructorArgs = s.schema.fields.zipWithIndex.map { case ((_, t), i) =>
              "*(reinterpret_cast<" + typeToString(t) + "*>((*ea)[" + i + "].get()))"
            }.mkString(", ")
          code = code + "    if (relation == program_base->get_relation_id(\"" + s.schema.name + "\"" + ")) { \n"
          code = code + "      event_args_t* ea = reinterpret_cast<event_args_t*>(eaList[i].get());\n"
          code = code + "      " + delta(s.schema.name) + "_entry e(" + constructorArgs + ", *(reinterpret_cast<" + typeToString(TypeLong) + "*>((*ea)[" + s.schema.fields.size + "].get())));\n"
          code = code + "      " + delta(s.schema.name) + ".addOrDelOnZero(e, *(reinterpret_cast<" + typeToString(TypeLong) + "*>((*ea)[" + s.schema.fields.size + "].get())));\n"
          code = code + "    }\n"
        }
        code = code +   "  }\n"
        for (s <- sources.filter(_.isStream)) {
          code = code + "  on_" + op + "_" + s.schema.name + "(" + delta(s.schema.name) + ");\n"
        }            
        code = code +   "}\n\n"

        val deltaRel = delta(name)
        val insertOp = stringIf(CppGen.EXPERIMENTAL_HASHMAP, "insert", "insert_nocheck")
        code +
        s"""|void unwrap_insert_${name}(const event_args_t& ea) {
            |  ${deltaRel}.clear(); 
            |  ${deltaRel}_entry e(${constructorArgs}, 1L);
            |  ${deltaRel}.${insertOp}(e);
            |  on_batch_update_${name}(${deltaRel});
            |}
            |
            |void unwrap_delete_${name}(const event_args_t& ea) {
            |  ${deltaRel}.clear(); 
            |  ${deltaRel}_entry e(${constructorArgs}, -1L);
            |  ${deltaRel}.${insertOp}(e);
            |  on_batch_update_${name}(${deltaRel});
            |}
            |""".stripMargin
      case _ =>
        s"""|void unwrap_${op}_${name}(const event_args_t& ea) {
            |  on_${op}_${name}(${constructorArgs});
            |}
            |""".stripMargin
    }
  })  
  // ------- Trigger generation (End)

  protected def emitMapTypes(s0: System) = {
    "/* Definitions of maps used for storing materialized views. */\n" + 
    stringIf(EXPERIMENTAL_RUNTIME_LIBRARY, {
      // 1. Entry type definitions for all streams
      s0.sources.filter(_.isStream).map { s =>
        emitEntryType(s.schema.name + "_entry", s.schema.fields, (VALUE_NAME, TypeLong))("")
      }.mkString("\n") 
    }) +
    {
      // 2. Maps (declared maps + tables + batch updates)
      s0.maps.filter(_.keys.size > 0) ++
      // 3. Queries without a map (with -F EXPRESSIVE-TLQS)
      s0.queries.filter {
        q => (q.expr.ovars.size > 0) && !s0.maps.exists(_.name == q.name)
      }.map {
        q => MapDef(q.name, q.expr.tp, q.expr.ovars, q.expr, LocalExp)
      }
    }.map(emitMapType).mkString("\n")
  }

  private def emitMapType(m: MapDef) = {
    val mapName = m.name
    val mapEntryType = mapName + "_entry"
    val mapType = mapName + "_map"
    val mapValueType = typeToString(m.tp)    
    val fields = m.keys ++ List(VALUE_NAME -> m.tp)
    val indices =     // primary + (multiple) secondary 
      ((0 until m.keys.size).toList -> true /* unique */) ::
      secondaryIndices(m.name).map(p => (p -> false /* non_unique */))   

    val sEntryType = {
      val sModifyFn = {
        val s = indices.map { case (cols, unique) =>
            val params = cols.map { case i => "const " + refTypeToString(fields(i)._2) + " c" + i }.mkString(", ")
            val body = cols.map { case i => fields(i)._1 + " = c" + i + "; "}.mkString
            s"FORCE_INLINE ${mapEntryType}& modify" + stringIf(!unique, getIndexId(mapName, cols)) + 
            "(" + params + ") { " + body + " return *this; }"
          }.mkString("\n")

        stringIf(s.nonEmpty, "\n" + s + "\n")
      }

      emitEntryType(mapEntryType, m.keys, (VALUE_NAME, m.tp))(sModifyFn)
    }

    val sIndexOpsType = indices.map { case (is, unique) =>
      val name = mapType + "key" + getIndexId(mapName, is) + "_idxfn"

      val sCombinators = is.take(EXPERIMENTAL_MAX_INDEX_VARS).map { idx =>
        "hash_combine(h, e." + fields(idx)._1 + ");"
      }.mkString("\n")

      val sCmpExpr = is.map { idx =>
        val (n, tp) = fields(idx)
        cmpFunc(tp, OpEq, "x." + n, "y." + n, false)
      }.mkString(" && ")

      s"""|struct ${name} {
          |  FORCE_INLINE static size_t hash(const ${mapEntryType}& e) {
          |    size_t h = 0;
          |${ind(sCombinators, 2)}
          |    return h;
          |  }
          |  
          |  FORCE_INLINE static bool equals(const ${mapEntryType}& x, const ${mapEntryType}& y) {
          |    return ${sCmpExpr};
          |  }
          |};
          |""".stripMargin
    }.mkString("\n")

    val sMapTypedefs = {
      if (EXPERIMENTAL_HASHMAP) {
        val sIndices = indices.map { case (is, unique) =>
            if (unique) "PrimaryHashIndex<" + mapEntryType + ", " + mapType + "key" + getIndexId(mapName, is) + "_idxfn>"
            else "SecondaryHashIndex<" + mapEntryType + ", " + mapType + "key" + getIndexId(mapName, is) + "_idxfn>"
          }.mkString(",\n")
        
        s"""|typedef MultiHashMap<${mapEntryType}, ${mapValueType}, 
            |${ind(sIndices)}
            |> ${mapType};
            |""".stripMargin
      }
      else {
        val sIndices = indices.map { case (is, unique) => 
            "HashIndex<" + mapEntryType + ", " + mapValueType + ", " + mapType + "key" + getIndexId(mapName, is) + "_idxfn, " + unique + ">"
          }.mkString(",\n")
        val sIndexTypedefs = indices.map { case (is, unique) =>
            "typedef HashIndex<" + mapEntryType + ", " + mapValueType + ", " + mapType + "key" + getIndexId(mapName, is) + "_idxfn, " + unique + "> HashIndex_" + mapType + "_" + getIndexId(mapName, is) + ";"
          }.mkString("\n")

        s"""|typedef MultiHashMap<${mapEntryType}, ${mapValueType},
            |${ind(sIndices)}
            |> ${mapType};
            |${sIndexTypedefs}
            |""".stripMargin
      }
    }

    s"""|${sEntryType}
        |${sIndexOpsType}
        |${sMapTypedefs}
        |""".stripMargin
  }

  private def emitEntryType(name: String, keys: List[(String, Type)], value: (String, Type)): (String => String) = {
    val fields = keys ++ List(value)

    val sFieldDefinitions = fields.map { case (n, t) => 
        typeToString(t) + " " + n + "; " 
      }.mkString + name + "* nxt; " + name + "* prv;"
          
    val sConstructorParams = fields.zipWithIndex.map { case ((_, tp), i) => 
        "const " + refTypeToString(tp) + " c" + i 
      }.mkString(", ")
    
    val sConstructorBody = fields.zipWithIndex.map { case ((n, _), i) => 
        n + " = c" + i + "; " 
      }.mkString

    val sInitializers = fields.map { case (n, tp) => 
        n + "(other." + n + "), "
      }.mkString + "nxt(nullptr), prv(nullptr)"

    val sStringInit = 
      keys.zipWithIndex.map { case ((n, tp), i) => tp match {
          case TypeLong | TypeDate => n + " = std::stol(f[" + i + "]);"
          case TypeDouble => n + " = std::stod(f[" + i + "]);"
          case TypeString => n + " = f[" + i + "];"
          case _ => sys.error("Unsupported type in fromString")
        }
      }.mkString(" ") + s" ${VALUE_NAME} = v; nxt = nullptr; prv = nullptr;" 

    val sSerialization = fields.map { case (n, _) =>
        s"""|ar << ELEM_SEPARATOR;
            |DBT_SERIALIZATION_NVP(ar, ${n});
            |""".stripMargin
      }.mkString

    (s => 
      s"""|struct ${name} {
          |  ${sFieldDefinitions}
          |
          |  explicit ${name}() : nxt(nullptr), prv(nullptr) { }
          |  explicit ${name}(${sConstructorParams}) { ${sConstructorBody} }
          |  ${name}(const ${name}& other) : ${sInitializers} { }
          |  // TODO: enable constructor
          |  //${name}(const std::vector<std::string>& f, const ${refTypeToString(value._2)} v) {
          |  //    /* if (f.size() < ${keys.size}) return; */
          |  //    ${sStringInit} 
          |  //}
          |${ind(s)}
          |  template<class Archive>
          |  void serialize(Archive& ar, const unsigned int version) const {
          |${ind(sSerialization, 2)}
          |  }
          |};
          |""".stripMargin)
  }

  private def emitTLQStructure(s0: System) = {  
    
    val sSerializeFn = {
      val body = s0.queries.map { q =>
        s"""|ar << "\\n";
            |const ${queryRefTypeToString(q)} _${q.name} = get_${q.name}();
            |dbtoaster::serialize_nvp_tabbed(ar, STRING(${q.name}), _${q.name}, "\\t");
            |""".stripMargin
      }.mkString("\n")

      s"""|/* Serialization code */
          |template<class Archive>
          |void serialize(Archive& ar, const unsigned int version) const {
          |${ind(body)}
          |}
          |""".stripMargin
    }

    val sTLQGetters = {
      val s = s0.queries.map { q =>
        val body = q.expr match {
          case MapRef(n, _, _, _) if (n == q.name) => 
            "return " + q.name + ";"
          case _ =>
            ctx = Ctx[(Type, String)]()
            if (q.expr.ovars.length == 0) {
              cpsExpr(q.expr, (v: String) => "return " + v + ";")
            }
            else {
              val localEntry = fresh("se")
              localEntries += ((localEntry, q.name + "_entry"))
              val localEntryArgs = q.expr.ovars.map(v => rn(v._1)).mkString(", ")            
              val sEvaluateExpr = cpsExpr(q.expr, (v: String) =>
                s"${q.name}.addOrDelOnZero(${localEntry}.modify(${localEntryArgs}), ${v});")

              s"""|${q.name}.clear();
                  |${sEvaluateExpr}
                  |return ${q.name};
                  |""".stripMargin
            }
        }
        s"""|const ${queryRefTypeToString(q)} get_${q.name}() const {
            |${ind(body)}
            |}
            |""".stripMargin
      }.mkString("\n")

      stringIf(s.nonEmpty, "/* Functions returning / computing the results of top level queries */\n" + s)
    }

    val sTLQDefinitions = emitTLQDefinitions(s0.queries) 

    val sDataDefinitions = 
      stringIf(isExpressiveTLQSEnabled, emitDataDefinitions(s0.maps, s0.queries))

    val sTLQMapInitializer = emitTLQMapInitializer(s0.maps, s0.queries)

    s"""|/* Defines top-level materialized views */
        |struct tlq_t {
        |  struct timeval t0, t; long tT, tN, tS;
        |  tlq_t(): tN(0), tS(0) ${sTLQMapInitializer} { 
        |    gettimeofday(&t0, NULL); 
        |  }
        |
        |${ind(sSerializeFn)}
        |
        |${ind(sTLQGetters)}
        |
        |protected:
        |${ind(sTLQDefinitions)}
        |${ind(sDataDefinitions)}
        |};
        |
        |""".stripMargin
  }

  protected def emitTLQMapInitializer(maps: List[MapDef], queries: List[Query]) = {
    // TLQ map initializer
    maps.filter { m => m.keys.size == 0 && queries.exists(_.name == m.name) }
        .map { m => ", " + m.name + "(" + zeroOfType(m.tp) + ")" }
        .mkString +
    stringIf(isExpressiveTLQSEnabled, {
      // Non-TLQ map initializer
      maps.filter { m => m.keys.size == 0 && !queries.exists(_.name == m.name) }
          .map { m => ", " + m.name + "(" + zeroOfType(m.tp) + ")" }
          .mkString
    })
  }

  protected def emitTLQDefinitions(queries: List[Query]) = {
    val s = queries.map { q => s"${queryTypeToString(q)} ${q.name};" }.mkString("\n")
    stringIf(s.nonEmpty, "/* Data structures used for storing / computing top-level queries */\n" + s)
  }

  private def emitIVMStructure(s0: System) = {

    val sTriggerFunctions = emitTriggerFunctions(s0)

    val sRegisterData = if (EXPERIMENTAL_RUNTIME_LIBRARY) "" else {

      val sRegisterMaps = {
        val s = s0.maps.map { m =>
            s"""pb.add_map<${mapTypeToString(m)}>("${m.name}", ${m.name});"""
          }.mkString("\n")

        stringIf(s.nonEmpty, "// Register maps\n" + s)
      }

      val sRegisterRelations = {
        val s = s0.sources.map { s => 
            s"""pb.add_relation("${s.schema.name}", ${if (s.isStream) "false" else "true"});"""
          }.mkString("\n")

        stringIf(s.nonEmpty, "// Register streams and tables\n" + s)
      }

      val sRegisterStreamTriggers = {
        val s = s0.triggers.filter(_.event != EventReady).map { _.event match {
          case EventBatchUpdate(Schema(n, _)) =>
            s"""|pb.add_trigger("${n}", batch_update, std::bind(&data_t::unwrap_batch_update_${n}, this, std::placeholders::_1));
                |pb.add_trigger("${n}", insert_tuple, std::bind(&data_t::unwrap_insert_${n}, this, std::placeholders::_1));
                |pb.add_trigger("${n}", delete_tuple, std::bind(&data_t::unwrap_delete_${n}, this, std::placeholders::_1));""".stripMargin
          case EventInsert(Schema(n, _)) => 
            s"""pb.add_trigger("${n}", insert_tuple, std::bind(&data_t::unwrap_insert_${n}, this, std::placeholders::_1));"""
          case EventDelete(Schema(n, _)) => 
            s"""pb.add_trigger("${n}", delete_tuple, std::bind(&data_t::unwrap_delete_${n}, this, std::placeholders::_1));"""
          case _ => ""
        }}.mkString("\n")

        stringIf(s.nonEmpty, "// Register stream triggers\n" + s)
      }

      val sRegisterTableTriggers = {          
        val s = s0.sources.filter(!_.isStream).map { s => 
            stringIf(isBatchModeActive,
              s"""pb.add_trigger("${s.schema.name}", batch_update, std::bind(&data_t::unwrap_batch_update_${s.schema.name}, this, std::placeholders::_1));\n"""
            ) + 
            s"""pb.add_trigger("${s.schema.name}", insert_tuple, std::bind(&data_t::unwrap_insert_${s.schema.name}, this, std::placeholders::_1));"""
          }.mkString("\n")

        stringIf(s.nonEmpty, "// Register table triggers\n" + s) 
      }

      s"""|/* Registering relations and trigger functions */
          |ProgramBase* program_base;
          |void register_data(ProgramBase& pb) {
          |  program_base = &pb;
          |
          |${ind(sRegisterMaps)}
          |
          |${ind(sRegisterRelations)}
          |
          |${ind(sRegisterStreamTriggers)}
          |
          |${ind(sRegisterTableTriggers)}
          |
          |}
          |""".stripMargin
    }

    val sDataDefinitions = 
      stringIf(!isExpressiveTLQSEnabled, emitDataDefinitions(s0.maps, s0.queries))

    s"""|/* Contains materialized views and processing (IVM) logic */
        |struct data_t : tlq_t {
        |
        |  data_t(): tlq_t()${emitNonTLQMapInitializer(s0.maps, s0.queries)} {
        |${ind(emitConstInits, 2)}
        |${ind(emitRegexInit, 2)}
        |  }
        |
        |${ind(emitRegexDestroy)}
        |
        |${ind(sRegisterData)}
        |
        |${ind(sTriggerFunctions)}
        |
        |private:
        |${ind(sDataDefinitions)}
        |};
        |""".stripMargin
  }

  protected def emitNonTLQMapInitializer(maps: List[MapDef], queries: List[Query]) =
    stringIf(!isExpressiveTLQSEnabled, {
      // Non-TLQ map initializer
      maps.filter { m => m.keys.size == 0 && !queries.exists(_.name == m.name) }
          .map { m => ", " + m.name + "(" + zeroOfType(m.tp) + ")" }
          .mkString
    })

  private def emitMainClass(s0: System) = stringIf(!EXPERIMENTAL_RUNTIME_LIBRARY, {
    s"""|/* Type definition providing a way to execute the sql program */
        |class Program : public ProgramBase {
        |  public:
        |    Program(int argc = 0, char* argv[] = 0) : ProgramBase(argc,argv) {
        |      data.register_data(*this);
        |
        |${ind(emitSourceDefinitions(s0.sources), 3)}
        |    }
        |
        |    /* Imports data for static tables and performs view initialization based on it. */
        |    void init() {
        |        table_multiplexer.init_source(run_opts->batch_size, run_opts->parallel, true);
        |        stream_multiplexer.init_source(run_opts->batch_size, run_opts->parallel, false);
        |
        |        ${stringIf(!cgOpts.printTiminingInfo, "// ")}struct timeval ts0, ts1, ts2;
        |        ${stringIf(!cgOpts.printTiminingInfo, "// ")}gettimeofday(&ts0, NULL);
        |        process_tables();
        |        ${stringIf(!cgOpts.printTiminingInfo, "// ")}gettimeofday(&ts1, NULL);
        |        ${stringIf(!cgOpts.printTiminingInfo, "// ")}long int et1 = (ts1.tv_sec - ts0.tv_sec) * 1000L + (ts1.tv_usec - ts0.tv_usec) / 1000;
        |        ${stringIf(!cgOpts.printTiminingInfo, "// ")}std::cout << "Populating static tables time: " << et1 << " (ms)" << std::endl;
        |
        |        data.on_system_ready_event();
        |        ${stringIf(!cgOpts.printTiminingInfo, "// ")}gettimeofday(&ts2, NULL);
        |        ${stringIf(!cgOpts.printTiminingInfo, "// ")}long int et2 = (ts2.tv_sec - ts1.tv_sec) * 1000L + (ts2.tv_usec - ts1.tv_usec) / 1000;
        |        ${stringIf(!cgOpts.printTiminingInfo, "// ")}std::cout << "OnSystemReady time: " << et2 << " (ms)" << std::endl;
        |
        |        gettimeofday(&data.t0, NULL);
        |    }
        |
        |    /* Saves a snapshot of the data required to obtain the results of top level queries. */
        |    snapshot_t take_snapshot() {
        |${ind(emitTakeSnapshotBody, 4)}
        |        return snapshot_t( d );
        |    }
        |
        |  protected:
        |    data_t data;
        |};
        |""".stripMargin +
        //
        stringIf(cgOpts.className != "Program",
          s"""|
              |class ${cgOpts.className} : public Program {
              |  public:
              |    ${cgOpts.className}(int argc = 0, char* argv[] = 0) : Program(argc, argv) { }
              |};""".stripMargin
        )
  })

  protected def emitTakeSnapshotBody = 
    s"""|${stringIf(!cgOpts.printTiminingInfo, "// ")}gettimeofday(&data.t, NULL);
        |${stringIf(!cgOpts.printTiminingInfo, "// ")}long int t = (data.t.tv_sec - data.t0.tv_sec) * 1000L + (data.t.tv_usec - data.t0.tv_usec) / 1000;
        |${stringIf(!cgOpts.printTiminingInfo, "// ")}std::cout << "Trigger running time: " << t << " (ms)" << std::endl;
        |
        |tlq_t* d = new tlq_t((tlq_t&) data);
        |${stringIf(cgOpts.isReleaseMode, "// ")}gettimeofday(&(d->t), NULL);
        |${stringIf(cgOpts.isReleaseMode, "// ")}d->tT = ((d->t).tv_sec - (d->t0).tv_sec) * 1000000L + ((d->t).tv_usec - (d->t0).tv_usec);
        |${stringIf(cgOpts.isReleaseMode, "// ")}printf(\"SAMPLE = ${cgOpts.dataset}, %ld, %ld, %ld\\n\", d->tT, d->tN, d->tS);
        |""".stripMargin

  private def emitSourceDefinition(s: Source): String = {
    val sourceId = fresh("source");

    val adaptorVar = sourceId + "_adaptor"    
    val sAdaptor = {
      val paramsVar = adaptorVar + "_params"
      val schemaTypes = s.schema.fields.map { _._2.toString }.mkString(",")

      s.adaptor.name match {
        case "ORDERBOOK" => {
          val (orderBookTypes, others) = 
            s.adaptor.options.partition { x => x._1 == "bids" || x._1 == "asks"}
          val orderBookType = orderBookTypes.size match {
            case 1 => orderBookTypes.head._1
            case 2 => "both"
          }
          val options = others ++ Map("schema" -> schemaTypes)
          val pairs = 
            "make_pair(\"book\", \"" + orderBookType + "\"), " +
            options.map { x => "make_pair(\"" + x._1 + "\", \"" + x._2 + "\")" }.mkString(", ")

          "pair<string,string> " + paramsVar + "[] = { " + pairs + " };\n" +
          "std::shared_ptr<order_books::order_book_adaptor> " + adaptorVar +
          "(new order_books::order_book_adaptor(" + 
          List("bids", "asks").map { x =>  
            if (s.adaptor.options.contains(x))
              "get_relation_id(\"" + s.adaptor.options(x) + "\"), "
            else "-1, " 
          }.mkString +
          (options.size + 1) + ", " + paramsVar + "));\n"
        }      
        case "CSV" => {
          val options = s.adaptor.options ++ Map("schema" -> schemaTypes, "deletions" -> cgOpts.datasetWithDeletions.toString)
          val pairs = options.map { x => "make_pair(\"" + x._1 + "\", \"" + x._2 + "\")" }.mkString(", ")

          "pair<string,string> " + paramsVar + "[] = { " + pairs + " };\n" +
          "std::shared_ptr<csv_adaptor> " + adaptorVar + 
          "(new csv_adaptor(get_relation_id(\"" + s.schema.name + "\"), " +
          options.size + ", " + paramsVar + "));\n"
        }
      }
    }

    val sourceSplitVar = sourceId + "_fd"
    val sFrameDescriptor = 
      "frame_descriptor " + sourceSplitVar + 
        (s.split match { 
          case SplitLine => "(\"\\n\")" 
          case SplitSep(sep) => "(\"" + sep + "\")" 
          case SplitSize(bytes) => "(" + bytes + ")" 
          case SplitPrefix(p) => "XXXXX(" + p + ")"   //XXXX for SplitPrefix
        }) + ";\n"

    val sourceFileVar = sourceId + "_file"
    val sFileSource = s.in match { case SourceFile(path) => 
      "std::shared_ptr<dbt_file_source> " + sourceFileVar + 
      "(new dbt_file_source(\"" + path + "\"," + sourceSplitVar + "," + adaptorVar + "));\n" 
    }
    
    val registerSource = "add_source(" + sourceFileVar + (if (s.isStream) ", false" else ", true") + ");\n"

    sAdaptor + sFrameDescriptor + sFileSource + registerSource
  }

  private def emitSourceDefinitions(sources: List[Source]) = {
    // one source generates BOTH asks and bids events
    def fixOrderbook(ss: List[Source]): List[Source] = { 
      val (obooks, others) = ss.partition { _.adaptor.name == "ORDERBOOK" }
      
      val grouped = HashMap[(Boolean, SourceIn), (Schema, Split, Map[String, String], LocalityType)]()     
      obooks.foreach { case Source(isStream, schema, in, split, adp, loc) =>
        val k = ((isStream, in))
        val v = (adp.options - "book") + ((adp.options.getOrElse("book", "bids"), schema.name))
        grouped.get(k) match {
          case Some(v2) => grouped += (k -> (schema, split, v2._3 ++ v, loc))
          case None => grouped += (k -> (schema, split, v, loc))
        }
      }
      grouped.toList.map { case ((isStream, in), (schema, split, opts, loc)) => 
        Source(isStream, schema, in, split, Adaptor("ORDERBOOK", opts), loc)
      } ::: others        
    }
    val fixed = fixOrderbook(sources)
    val s = fixed.filter{!_.isStream}.map(emitSourceDefinition).mkString("\n") + "\n" +
            fixed.filter{_.isStream}.map(emitSourceDefinition).mkString("\n")

    stringIf(s.nonEmpty, "/* Specifying data sources */\n" + s)
  }

  // --------------  

  private var unionDepth = 0
  
  // extract cond and then branch of "if (c) t else 0"
  private def extractBooleanExp(s: String): Option[(String, String)] = 
    if (!s.startsWith("(/*if */(")) None 
    else {      
      val posInit = "(/*if */(".length
      var pos = posInit
      var nestingLvl = 1
      while (nestingLvl > 0) {
        if (s(pos) == '(') nestingLvl += 1 
        else if (s(pos)==')') nestingLvl -= 1
        pos += 1
      }
      Some(s.substring(posInit, pos - 1), s.substring(pos + " ? ".length, s.lastIndexOf(":") - 1))
    }

  private def addToTempMapFunc(ksTp: List[Type], vsTp: Type, m: String, ks: List[String], vs: String) = {
    val localEntry = fresh("st")
    tempEntryTypes += ((ksTp, vsTp))
    localEntries += ((localEntry, tempEntryTypeName(ksTp, vsTp)))

    if (EXPERIMENTAL_HASHMAP) {
      extractBooleanExp(vs) match {
        case Some((c, t)) =>
          s"(/*if */(" + c + ") ? " + m + ".add(" + localEntry + ".modify(" + ks.map(rn).mkString(", ") + "), " + t + ") : (void)0);\n"
        case _ =>
          m + ".add(" + localEntry + ".modify(" + ks.map(rn).mkString(", ") + "), " + vs + ");\n"
      }
    }
    else {
      extractBooleanExp(vs) match {
        case Some((c, t)) =>
          "(/*if */(" + c + ") ? add_to_temp_map" + /*"<"+tempEntryTypeName(ksTp, vsTp)+">"+*/ 
          "(" + m + ", " + localEntry + ".modify(" + ks.map(rn).mkString(", ") + "), " + t + ") : (void)0);\n"
        case _ =>
          "add_to_temp_map" + /*"<"+tempEntryTypeName(ksTp, vsTp)+">"+*/
          "(" + m + ", " + localEntry + ".modify(" + ks.map(rn).mkString(", ") + "), " + vs +");\n"
      }
    }
  }

  private def applyFunc(co: String => String, fn1: String, tp: Type, as1: List[Expr]) = {
    val (as, fn) = fn1 match {
      case "regexp_match" if (ENABLE_REGEXP_PARTIAL_EVAL && 
                              as1.head.isInstanceOf[Const] && 
                              !as1.tail.head.isInstanceOf[Const]) => 
        val regex = as1.head.asInstanceOf[Const].v
        val preg0 = regexpCacheMap.getOrElseUpdate(regex, fresh("preg"))
        (as1.tail, "preg_match(" + preg0 + ",")
      case "date_part" => as1.head.asInstanceOf[Const].v.toLowerCase match {
          case "year"  => (as1.tail, "date_year" + "(")
          case "month" => (as1.tail, "date_month" + "(")
          case "day"   => (as1.tail, "date_day" + "(")
          case p       => throw new Exception("Invalid date part: " + p)
        }
      case _ => (as1, fn1 + "(")
    }
    // hoist constants resulting from function application
    if (as.forall(_.isInstanceOf[Const])) {
      co(hoistedConsts.getOrElseUpdate(Apply(fn1, tp, as1), fresh("c"))) 
    }
    else {
      var c = co
      as.zipWithIndex.reverse.foreach { case (a, i) => 
        val c0 = c
        c = (p: String) => cpsExpr(a, (v: String) => 
          c0(p + stringIf(i > 0, ", ") + v + stringIf(i == as.size - 1, ")"))) 
      }
      c("U" + fn) 
    }
  }  

  private def cmpFunc(tp: Type, op: OpCmp, arg1: String, arg2: String, withIfThenElse: Boolean = true) = 
    if(withIfThenElse)
      "(/*if */(" + arg1 + " " + cmpToString(op) + " " + arg2 + ")" + " ? 1L : 0L)"
    else
      arg1 + " " + cmpToString(op) + " " + arg2


  // Generate code bottom-up using delimited CPS and a list of bound variables
  //   ex:expression to convert
  //   co:delimited continuation (code with 'holes' to be filled by expression) similar to Rep[Expr]=>Rep[Unit]
  //   am:shared aggregation map for Add and AggSum, avoiding useless intermediate map where possible
  // override 
  private def cpsExpr(ex: Expr, co: String => String = (v: String) => v, am: Option[List[(String, Type)]] = None): String = ex match {
    case Ref(n) => co(rn(n))

    case Const(tp, v) => tp match {
      case TypeLong => co(v + "L")
      case TypeString => cpsExpr(Apply("STRING_TYPE", TypeString, List(ex)), co, am)
      case _ => co(v)
    }

    case Exists(e) => cpsExpr(e, (v: String) => 
      co(cmpFunc(TypeLong, OpNe, v, zeroOfType(ex.tp))))

    case Cmp(l, r, op) =>
      co(cpsExpr(l, (ll: String) => cpsExpr(r, (rr: String) => cmpFunc(l.tp, op, ll, rr))))

    case CmpOrList(l, r) =>
      co(cpsExpr(l, (ll: String) =>
        "(/*if */((" +
        r.map(x => cpsExpr(x, (rr: String) => "(" + ll + " == " + rr + ")"))
         .mkString(" || ") +
        ")) ? 1L : 0L)"
      ))

    case Apply(fn, tp, as) => applyFunc(co, fn, tp, as)

    case MapRef(mapName, tp, ks, isTemp) =>
      val (ko, ki) = ks.zipWithIndex.partition { case((n, _), _) => ctx.contains(n) }
      val mapType = mapName + "_map"
      val mapEntryType = mapName + "_entry"
      
      if (ks.size == 0) { // variable
        if (ctx contains mapName) co(rn(mapName)) else co(mapName)
      } 
      else if (ki.size == 0) {
        val localEntry = fresh("se")
        localEntries += ((localEntry, mapEntryType))
        val argList = (ks map (x => rn(x._1))).mkString(", ")
        co(s"${mapName}.getValueOrDefault(${localEntry}.modify(${argList}))") // all keys are bound
      } 
      else {
        val (v0, e0) = (fresh("v"), fresh("e"))

        ctx.add(ks.filter(x => !ctx.contains(x._1)).map(x => (x._1, (x._2, x._1))).toMap)

        if (!isTemp) { // slice or foreach
          val body = 
            ki.map { case ((k, ktp), i) => 
              typeToString(ktp) + " " + rn(k) + " = " + e0 + "->" + mapDefs(mapName).keys(i)._1 + ";\n"
            }.mkString + 
            typeToString(tp) + " " + v0 + " = " + e0 + "->" + VALUE_NAME + ";\n" +
            co(v0)

          if (ko.size > 0) { //slice
            if (EXPERIMENTAL_RUNTIME_LIBRARY && deltaRelationNames.contains(mapName)) {
              sys.error("Delta relation requires secondary indices. Turn off experimental runtime library.")
            }

            val is = ko.map(_._2)
            val idxIndex = registerSecondaryIndex(mapName, is) + 1 //+1 because index 0 is the unique index
            val localEntry = fresh("se")
            localEntries += ((localEntry, mapEntryType))
            val sKeys = ko.map(x => rn(x._1._1)).mkString(", ")
            val n0 = fresh("n")

            if (EXPERIMENTAL_HASHMAP) {
              s"""|{ //slice
                  |  const SecondaryIdxNode<${mapEntryType}>* ${n0}_head = static_cast<const SecondaryIdxNode<${mapEntryType}>*>(${mapName}.slice(${localEntry}.modify${getIndexId(mapName,is)}(${sKeys}), ${idxIndex - 1}));
                  |  const SecondaryIdxNode<${mapEntryType}>* ${n0} = ${n0}_head;
                  |  ${mapEntryType}* ${e0};
                  |  while (${n0}) {
                  |    ${e0} = ${n0}->obj;
                  |${ind(body, 2)}
                  |    ${n0} = (${n0} != ${n0}_head ? ${n0}->nxt : ${n0}->child);
                  |  }
                  |}
                  |""".stripMargin
            }
            else {
              val (h0, idx0) = (fresh("h"), fresh("i"))
              val idxName = "HashIndex_" + mapType + "_" + getIndexId(mapName, is)
              val idxFn = mapType + "key" + getIndexId(mapName, is) + "_idxfn"
              s"""|{ //slice
                  |  const HASH_RES_t ${h0} = ${idxFn}::hash(${localEntry}.modify${getIndexId(mapName,is)}(${sKeys}));
                  |  const ${idxName}* ${idx0} = static_cast<${idxName}*>(${mapName}.index[${idxIndex}]);
                  |  ${idxName}::IdxNode* ${n0} = &(${idx0}->buckets_[${h0} & ${idx0}->mask_]);
                  |  ${mapEntryType}* ${e0};
                  |  do if ((${e0} = ${n0}->obj) && ${h0} == ${n0}->hash && ${idxFn}::equals(${localEntry}, *${e0})) {
                  |${ind(body, 2)}
                  |  } while ((${n0} = ${n0}->nxt));
                  |}
                  |""".stripMargin
            }
          } 
          else { //foreach
            if (EXPERIMENTAL_RUNTIME_LIBRARY && deltaRelationNames.contains(mapName)) {
              s"""|{ //foreach
                  |  for (std::vector<${mapEntryType}>::iterator ${e0} = begin; ${e0} != end; ${e0}++) {
                  |${ind(body, 2)}
                  |  }
                  |}
                  |""".stripMargin                
            }
            else {
              s"""|{ //foreach
                  |  ${mapEntryType}* ${e0} = ${mapName}.head;
                  |  while (${e0}) {
                  |${ind(body, 2)}
                  |    ${e0} = ${e0}->nxt;
                  |  }
                  |}
                  |""".stripMargin
            }
          }
        } 
        else { // only foreach for Temp map
          val localVars = 
            ki.map { case ((k, tp), i) => 
              s"${typeToString(tp)} ${rn(k)} = ${e0}->_${(i + 1)};"
            }.mkString("\n") 

          s"""|{ // temp foreach
              |  ${tempEntryTypeName(ks.map(_._2), tp)}* ${e0} = ${mapName}.head;
              |  while(${e0}) {
              |${ind(localVars, 2)} 
              |    ${typeToString(tp)} ${v0} = ${e0}->${VALUE_NAME}; 
              |
              |${ind(co(v0), 2)}
              |
              |    ${e0} = ${e0}->nxt;
              |  }
              |}
              |""".stripMargin
        }
      }

    // "1L" is the neutral element for multiplication, and chaining is done with multiplication
    case Lift(n, e) =>
      // Mul(Lift(x,3),Mul(Lift(x,4),x)) ==> (x=3;x) == (x=4;x)
      if (ctx.contains(n)) {
        cpsExpr(e, (v: String) => co(cmpFunc(TypeLong, OpEq, rn(n), v)), am)
      }
      else e match {
        case Ref(n2) => 
          ctx.add(n, (e.tp, rn(n2)))
          co("1L")    // de-aliasing         
        // This renaming is required
        case _ =>
          ctx.add(n, (e.tp, fresh("l")))
          cpsExpr(e, (v: String) => typeToString(e.tp) + " " + rn(n) + " = " + v + ";\n" + co("1L"), am)
      }

    // Mul(el,er)
    // ==
    //   Mul( (el,ctx0) -> (vl,ctx1) , (er,ctx1) -> (vr,ctx2) ) 
    //    ==>
    //   (v = vl * vr , ctx2)
    case Mul(el, er) => 
      def vx(vl: String, vr: String) = 
        if (vl == "1L") vr else if (vr == "1L") vl else "(" + vl + " * " + vr + ")"

      cpsExpr(el, (vl: String) => {
        var ifcond = ""
        val body = cpsExpr(er, (vr: String) => {
          (extractBooleanExp(vl), extractBooleanExp(vr)) match {
              case (Some((cl,tl)), Some((cr,tr))) =>
                if (unionDepth == 0) { ifcond = cl;  co("(/*if */(" + cr + ") ? " + vx(vl,tr) + " : " + zeroOfType(ex.tp) + ")") }
                else co("(/*if */(" + cl + " && " + cr + ") ? " + vx(tl,tr) + " : " + zeroOfType(ex.tp) + ")")

              case (Some((cl,tl)), _) =>
                if (unionDepth == 0) { ifcond = cl; co(vx(tl,vr)) }
                else co("(/*if */(" + cl + ") ? " + vx(tl,vr) + " : " + zeroOfType(ex.tp) + ")")

              case (_, Some((cr,tr))) =>
                co("(/*if */(" + cr + ") ? " + vx(vl,tr) + " : " + zeroOfType(ex.tp) + ")")

              case _ => co(vx(vl,vr))
            }
          }, am)
          if (ifcond == "") body else "if (" + ifcond + ") {\n" + ind(body) + "\n}\n"
        }, am)

    // Add(el,er)
    // ==
    //   Add( (el,ctx0) -> (vl,ctx1) , (er,ctx0) -> (vr,ctx2) ) 
    //         <-------- L -------->    <-------- R -------->
    //    (add - if there's no free variable) ==>
    //   (v = vl + vr , ctx0)
    //    (union - if there are some free variables) ==>
    //   T = Map[....]
    //   foreach vl in L, T += vl
    //   foreach vr in R, T += vr
    //   foreach t in T, co(t) 
    case a @ Add(el,er) =>      
      val agg = a.schema._2.filter { case (n,t) => !ctx.contains(n) }
      val s =
        if (agg == Nil) {
          val cur = ctx.save
          unionDepth += 1
          cpsExpr(el, (vl: String) => {
            ctx.load(cur)
            cpsExpr(er, (vr: String) => {
              ctx.load(cur)
              unionDepth -= 1
              co(s"(${vl} + ${vr})")
            }, am)
          }, am)
        }
        else am match {
          case Some(t) if t.toSet.subsetOf(agg.toSet) =>
            val cur = ctx.save
            unionDepth += 1
            val s1 = cpsExpr(el, co, am)
            ctx.load(cur)
            val s2 = cpsExpr(er, co, am)
            ctx.load(cur)
            unionDepth -= 1
            (s1 + s2)
          case _ =>
            val acc = fresh("_c")
            val ks = agg.map(_._1)
            val ksTp = agg.map(_._2)
            val cur = ctx.save
            unionDepth += 1
            val s1 = cpsExpr(el, (v: String) => addToTempMapFunc(ksTp, a.tp, acc, ks, v), Some(agg))
            ctx.load(cur)
            val s2 = cpsExpr(er, (v: String) => addToTempMapFunc(ksTp, a.tp, acc, ks, v), Some(agg))
            ctx.load(cur)
            unionDepth -= 1
            ( initializeTempMap(acc, a.tp, agg.map(_._2)) +
              s1 +
              s2 +
              cpsExpr(MapRef(acc, a.tp, agg, true), co) )
        }
      s

    case a @ AggSum(ks, e) =>
      val aks = ks.filter { case(n, t) => !ctx.contains(n) }
      if (aks.size == 0) {
        val cur = ctx.save
        val aggVar = fresh("agg")
        
        initializeTempMap(aggVar, a.tp) +
        cpsExpr(e, (v: String) =>
          extractBooleanExp(v) match {
            case Some((c,t)) =>
              "(/*if */(" + c + ") ? " + aggVar + " += " + t + " : " + zeroOfType(a.tp) + ");\n"
            case _ =>
              aggVar + " += " + v + ";\n"
          }) +
        { ctx.load(cur); co(aggVar) }
      } 
      else am match {
        case Some(t) if t.toSet.subsetOf(aks.toSet) => 
          cpsExpr(e, co, am)
        case _ =>
          val aggVar = fresh("agg")
          val tmp = Some(aks)     // declare this as summing target
          val cur = ctx.save
  
          initializeTempMap(aggVar, e.tp, aks.map(_._2)) +
          cpsExpr(e, (v: String) => 
            addToTempMapFunc(aks.map(_._2), e.tp, aggVar, aks.map(_._1), v), tmp) +
          { ctx.load(cur); cpsExpr(MapRef(aggVar, e.tp, aks, true), co) }
      }

    case Repartition(ks, e) => cpsExpr(e, (v: String) => co(v))
    
    case Gather(e) => cpsExpr(e, (v: String) => co(v))
  
    case _ => sys.error("Don't know how to generate " + ex)
  }


  def apply(s0: System): String = {
    implicit val s0_ = s0

    freshClear()

    mapDefs = s0.maps.map { m => (m.name -> m) }.toMap

    deltaRelationNames = s0.triggers.flatMap(_.event match {
      case EventBatchUpdate(s) => List(delta(s.name))
      case _ => Nil
    }).toSet

    isBatchModeActive = deltaRelationNames.nonEmpty

    isExpressiveTLQSEnabled = s0.queries.exists { q => 
      q.expr match { 
        case MapRef(n, _, _, _) => (n != q.name)
        case _ => true
      }
    }

    prepareCodegen(s0)
 
    val sIVMStructure = emitIVMStructure(s0)
    
    val sTLQStructure = emitTLQStructure(s0)

    val sIncludeHeaders = emitIncludeHeaders

    val sRelationTypeDirectives = 
      s0.sources.map { s => 
        if (s.isStream) 
          s"#define RELATION_${s.schema.name.toUpperCase}_DYNAMIC" 
        else 
          s"#define RELATION_${s.schema.name.toUpperCase}_STATIC"
      }.mkString("\n")

    // Generating the entire file
    s"""|${sIncludeHeaders}
        |${sRelationTypeDirectives}
        |
        |namespace dbtoaster {
        |
        |${ind(emitMapTypes(s0))}
        |
        |${ind(emitTempEntryTypes)}
        |
        |${ind(sTLQStructure)}
        |
        |${ind(sIVMStructure)}
        |
        |${ind(emitMainClass(s0))}
        |
        |}""".stripMargin    
  }

  protected def prepareCodegen(s0: System): Unit = {}

  protected def emitIncludeHeaders = 
    stringIf(!EXPERIMENTAL_HASHMAP, "#define USE_OLD_MAP\n") +
    stringIf(EXPERIMENTAL_RUNTIME_LIBRARY,
      s"""|#include <sys/time.h>
          |#include <vector>
          |#include "macro.hpp"
          |#include "types.hpp"
          |#include "functions.hpp"
          |#include "hash.hpp"
          |#include "mmap.hpp"
          |#include "serialization.hpp"
          |""".stripMargin,
      s"""|#include "program_base.hpp"
          |#include "hpds/KDouble.hpp"
          |#include "hash.hpp"
          |#include "mmap/mmap.hpp"
          |#include "hpds/pstring.hpp"
          |#include "hpds/pstringops.hpp"
          |""".stripMargin)
}
