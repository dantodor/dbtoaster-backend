package ddbt.codegen
import ddbt.codegen.lms._
import ddbt.ast._
import ddbt.lib._

/**
 * This code generator is similar to ScalaGen but targets LMS graph (IR) to
 * optimize further before actually emitting Scala code for the trigger bodies.
 *
 * @author Mohammad Dashti, TCK
 */

class LMSGen(cls:String="Query") extends ScalaGen(cls) {
  import ddbt.ast.M3._
  import ddbt.Utils.{ind,tup,fresh,freshClear} // common functions
  import ManifestHelper.{man,zero,manEntry}

  val impl = ScalaExpGen
  import impl.Rep

  var cx : Ctx[Rep[_]] = null

  import ddbt.lib.store._
  def me(ks:List[Type],v:Type=null):Manifest[Entry] = manEntry(if (v==null) ks else ks:::List(v)).asInstanceOf[Manifest[Entry]]
  def mapProxy(m:Rep[_]) = impl.store2StoreOpsCls(m.asInstanceOf[Rep[Store[Entry]]])


  // Expression CPS transformation from M3 AST to LMS graph representation
  //   ex : expression to convert
  //   co : continuation in which the result should be plugged
  //   am : shared aggregation map for Add and AggSum, avoiding useless intermediate map where possible
  def expr(ex:Expr, co: Rep[_]=>Rep[Unit], am:Option[List[(String,Type)]]=None):Rep[Unit] = ex match {
    case Ref(n) => co(cx(n))
    case Const(tp,v) => ex.tp match {
      case TypeLong => co(impl.unit(v.toLong))
      case TypeDouble => co(impl.unit(v.toDouble))
      case TypeString => co(impl.unit(v))
      case TypeDate => sys.error("No date constant conversion") //co(impl.unit(new java.util.Date()))
      case _ => sys.error("Unsupported type "+tp)
    }
    case Mul(l,r) => expr(l,(vl:Rep[_])=> expr(r,(vr:Rep[_]) => co(mul(vl,vr,ex.tp)),am),am)
    case a@Add(l,r) =>
      if (a.agg==Nil) { val cur=cx.save; expr(l,(vl:Rep[_])=>{ cx.load(cur); expr(r,(vr:Rep[_])=>{ cx.load(cur); co(add(vl,vr,ex.tp)) },am)},am) }
      else am match {
        case Some(t) if t.toSet==a.agg.toSet => val cur=cx.save; expr(l,co,am); cx.load(cur); expr(r,co,am); cx.load(cur);
        case _ =>
          implicit val mE=me(a.agg.map(_._2),a.tp)
          val acc = impl.m3temp()(mE)
          val inCo = (v:Rep[_]) => impl.m3add(acc,impl.newEntry( (a.agg.map(x=>cx(x._1))++List(v)) : _*)(mE))(mE)
          val cur = cx.save
          expr(l,inCo,Some(a.agg)); cx.load(cur)
          expr(r,inCo,Some(a.agg)); cx.load(cur)
          foreach(acc,a.agg,a.tp,co)
      }
    case Cmp(l,r,op) => expr(l,(vl:Rep[_]) => expr(r,(vr:Rep[_]) => co(cmp(vl,op,vr,ex.tp)) )) // formally, we should take the derived type from left and right, but this makes no difference to LMS
    case Exists(e) => expr(e,(ve:Rep[_]) => co(impl.__ifThenElse(impl.notequals(ve,impl.unit(0L)),impl.unit(1L),impl.unit(0L))))
    case Lift(n,e) => expr(e,(ve:Rep[_]) => if (cx.contains(n)) co(cmp(cx(n),OpEq,ve,e.tp)) else { cx.add(Map((n,ve))); co(impl.unit(1L)) })
    case a@Apply(fn,tp,as) =>
      def app(es:List[Expr],vs:List[Rep[_]]):Rep[Unit] = es match {
        case x :: xs => expr(x,(v:Rep[_]) => app(xs,v::vs))
        case Nil => co(impl.m3apply(fn,vs.reverse,tp))
      }
      //if (as.forall(_.isInstanceOf[Const])) co(impl.named(constApply(a),tp,false)) // hoist constants resulting from function application
      //else
       app(as,Nil)
    case m@MapRef(n,tp,ks) =>
      val (ko,ki) = ks.zipWithIndex.partition{case(k,i)=>cx.contains(k)}
      val proxy = mapProxy(cx(n))
      if(ks.size == 0) { // variable
        co(cx(n))
      } else if(ki.size == 0) { // all keys are bound
        val z = impl.unit(zero(tp))
        val vs:List[Rep[_]] = ks.map(cx).toList ::: List(z)
        //XXX remove value using sampleEntry
        val e = impl.sampleFullEntry(vs : _*)
        val r = proxy.get(e,0)
        impl.__ifThenElse(impl.__equal(r,impl.unit(null)),co(z),co(r.get(ks.size+1)))
      } else { // we need to iterate over all keys not bound (ki)
        if (ko.size==0) proxy.foreach(co)
        else {
          implicit val mE=me(m.tks,tp)
          val vs = (ks zip m.tks).map{ case (n,t)=>if(cx.contains(n)) cx(n) else impl.unit(t.zero) }.toList ::: List(impl.unit(tp.zero))
          proxy.slice(impl.sampleFullEntry(vs : _*)(mE),co) // XXX: figure out the slice id
        }
      }
    case a@AggSum(ks,e) =>
      val agg_keys = (ks zip a.tks).filter{ case (n,t)=> !cx.contains(n) } // the aggregation is only made on free variables
      if (agg_keys.size==0) { // Accumulate expr(e) in the acc, returns (Rep[Unit],ctx) and we ignore ctx
        val acc:impl.Var[_] = impl.var_new(impl.unit(zero(ex.tp)))
        val cur=cx.save; expr(e,(v:Rep[_]) => impl.var_plusequals(acc, v),None); cx.load(cur); co(acc)
      } else {
        implicit val mE=me(a.tks,ex.tp)
        val acc = impl.m3temp()(mE)
        val cur = cx.save

        val coAcc = (v:Rep[_]) => {
          val vs:List[Rep[_]] = agg_keys.map(x=>cx(x._1)).toList ::: List(v)
          impl.m3add(acc, impl.newEntry(vs : _*))
        }
        expr(e,coAcc,Some(agg_keys)); cx.load(cur) // returns (Rep[Unit],ctx) and we ignore ctx
        am match {
          case Some(t) if (t.toSet==agg_keys.toSet) => expr(e,co,am)
          case _ => foreach(acc,agg_keys,a.tp,co,"a")
        }
      }
    case _ => sys.error("Unimplemented: "+ex)
  }

  def foreach(map:Rep[_],keys:List[(String,Type)],value_tp:Type,co:Rep[_]=>Rep[Unit],prefix:String=""):Rep[Unit] = {
    implicit val mE = manEntry(keys.map(_._2) ::: List(value_tp))
    val proxy = mapProxy(map)
    proxy.foreach{ e:Rep[Entry] =>
      cx.add(keys.zipWithIndex.filter(x=> !cx.contains(x._1._1)).map { case ((n,t),i) => (n,e.get(i)) }.toMap)
      co(e.get(keys.size+1))
    }
  }

  def accessTuple(tuple: Rep[_],elemTp:Type,sz: Int, idx: Int)(implicit pos: scala.reflect.SourceContext): Rep[_] = sz match {
      case 1 => tuple
      case _ => impl.getClass.getDeclaredMethod("tuple%d_get%d".format(sz,idx+1),Class.forName("java.lang.Object"), Class.forName("scala.reflect.Manifest"), Class.forName("scala.reflect.SourceContext"))
                 .invoke(impl, tuple, man(elemTp), pos).asInstanceOf[Rep[_]]
  }

  def mul(l:Rep[_], r:Rep[_], tp:Type) = {
    @inline def times[T:Numeric:Manifest]() = impl.numeric_times[T](l.asInstanceOf[Rep[T]],r.asInstanceOf[Rep[T]])
    tp match {
      case TypeLong => times[Long]()
      case TypeDouble => times[Double]()
      case _ => sys.error("Mul(l,r) only allowed on numeric types")
    }
  }

  def add(l:Rep[_], r:Rep[_], tp:Type) = {
    @inline def plus[T:Numeric:Manifest]() = impl.numeric_plus[T](l.asInstanceOf[Rep[T]],r.asInstanceOf[Rep[T]])
    tp match {
      case TypeLong => plus[Long]()
      case TypeDouble => plus[Double]()
      case _ => sys.error("Add(l,r) only allowed on numeric types")
    }
  }

  def cmp(l:Rep[_], op:OpCmp, r:Rep[_], tp:Type): Rep[Long] = {
    @inline def cmp2[T:Ordering:Manifest](vl:Rep[_],vr:Rep[_]): Rep[Boolean] = {
      val (ll,rr)=(vl.asInstanceOf[Rep[T]],vr.asInstanceOf[Rep[T]])
      op match {
        case OpEq => impl.equals[T,T](ll,rr)
        case OpNe => impl.notequals[T,T](ll,rr)
        case OpGt => impl.ordering_gt[T](ll,rr)
        case OpGe => impl.ordering_gteq[T](ll,rr)
      }
    }
    impl.__ifThenElse(tp match {
      case TypeLong => cmp2[Long](l,r)
      case TypeDouble => cmp2[Double](l,r)
      case TypeString => cmp2[String](l,r)
      case TypeDate => cmp2[Long](impl.dtGetTime(l.asInstanceOf[Rep[java.util.Date]]),impl.dtGetTime(r.asInstanceOf[Rep[java.util.Date]]))
      case _ => sys.error("Unsupported type")
    },impl.unit(1L),impl.unit(0L))
  }

  // Trigger code generation
  def genTriggerLMS(t:Trigger) = {
    val (name,args) = t.evt match {
      case EvtReady => ("SystemReady",Nil)
      case EvtAdd(Schema(n,cs)) => ("Add"+n,cs)
      case EvtDel(Schema(n,cs)) => ("Del"+n,cs)
    }

    val block = impl.reifyEffects {
      // Trigger context: global maps + trigger arguments
      cx = Ctx((
        //maps.map{ case (name,MapDef(_,tp,keys,_)) => if (keys.size==0) (name,impl.namedM3Var(name,tp)(manifest[Any])) else (name,impl.namedM3Map(name,keys.map(_._2),tp,sx.getOrElse(name,List[List[Int]]()))(manifest[Any],manifest[Any])) }.toList union
        args.map{ case (name,tp) => (name,impl.named(name,tp)) }
      ).toMap)
      // Execute each statement
      t.stmts.map {
        case StmtMap(m,e,op,oi) => cx.load()
          val mm = cx(m.name)
          if (op==OpSet && m.keys.size>0) mapProxy(mm).clear
/*
          oi match { case None => case Some(ie) =>
            expr(ie,(r:Rep[_]) => { val keys = m.keys.map(cx)
               impl.__ifThenElse(impl.equals(  impl.m3get(mm,keys,m.tp),impl.unit(0L)),impl.m3set(mm,keys,r),impl.unit(()))
            })
          }
          cx.load()
*/
          expr(e,(r:Rep[_]) => op match {
            case OpAdd => if (m.keys.size==0) impl.var_plusequals(mm.asInstanceOf[impl.Var[_]],r) else {
              val vs:List[Rep[_]] = m.keys.map(cx).toList ::: List(r)
              impl.m3add(mm.asInstanceOf[Rep[Store[Entry]]], impl.newEntry(vs : _*))
            }
            case OpSet => if (m.keys.size==0) impl.__assign(mm,r) else {
              val vs:List[Rep[_]] = m.keys.map(cx).toList ::: List(r)
              // XXX: delete previous value
              impl.stInsert(mm.asInstanceOf[Rep[Store[Entry]]], impl.newEntry(vs : _*))
              //impl.m3set(mm,m.keys.map(cx),r)
            }
          }, if (op==OpAdd) Some(m.keys zip m.tks) else None)
        case _ => sys.error("Unimplemented") // we leave room for other type of events
      }
      impl.unit(())
    }
    cx = null; ("on"+name+"("+args.map{a=>a._1+":"+a._2.toScala} .mkString(", ")+")",block)
  }

  override def toMapFunction(q: Query) = {
    if(M3MapCommons.isInliningHigherThanNone) {
      //m = map
      val map = q.name
      val nodeName = map+"_node"
      val m = maps(q.name)
      val mapKeys = m.keys.map(_._2)
      val mapValue = m.tp
      val indexList = sx.getOrElse(map,List[List[Int]]())

      //val entryCls = M3MapCommons.entryClassName(q.tp, q.keys, sx.getOrElse(q.name,List[List[Int]]()))
      val entryCls = "("+tup(mapKeys.map(_.toScala))+","+q.map.tp.toScala+")"
      val res = nodeName+"_mres"
      val i = nodeName+"_mi"
      val len = nodeName+"_mlen"
      val e = nodeName+"_me"
      "{\n" +
      "  //TOMAP\n" +
      "  var "+res+":scala.collection.mutable.ArrayBuffer["+entryCls+"] = new scala.collection.mutable.ArrayBuffer["+entryCls+"]("+M3MapCommons.genMapSize(map)+");\n" +
      "  var "+i+":Int = 0\n" +
      "  val "+len+":Int = "+map+".length\n" +
      "  while("+i+" < "+len+") {\n" +
      "    var "+e+":"+M3MapCommons.entryClassName(mapValue,mapKeys,indexList)+" = "+map+"("+i+")\n" +
      "    while("+e+" != null) {\n"+
      "      "+res+" += ("+tup(mapKeys.zipWithIndex.map{ case (_,i) => e+"._"+(i+1) })+" -> "+e+".v)\n"+
      "      "+e+" = "+e+".next\n" +
      "    }\n" +
      "    "+i+" += 1\n" +
      "  }\n" +
      "  " + res + "\n" +
      "}.toMap"
    } else {
      super.toMapFunction(q)
    }
  }

  override def genMap(m:MapDef):String = {
    if(M3MapCommons.isInliningHigherThanNone) {
      if (m.keys.size==0) M3MapCommons.createM3VarDefinition(m.name, m.tp)+";"
      else {
        val keys = m.keys.map(_._2)
        val indexList = sx.getOrElse(m.name,List[List[Int]]())
        M3MapCommons.createM3NamedMapDefinition(m.name,m.tp,keys,indexList)
      }
    } else {
      super.genMap(m)
    }
  }

  override def genInitializationFor(map:String, keyNames:List[(String,Type)], keyNamesConcat: String) = {
    if(M3MapCommons.isInliningHigherThanNone) {
      //val theMap = maps(map)
      //val mapKeys = theMap.keys.map(_._2)
      val indexList = sx.getOrElse(map,List[List[Int]]())
      M3MapCommons.genGenericAddNamedMap(true,false,"","",map+"_node",map,keyNames.map(_._2),TypeLong,indexList,keyNames.map(_._1),"1")
    } else {
      super.genInitializationFor(map, keyNames, keyNamesConcat)
    }
  }

  // Expose the maps of the system being generated
  var maps = Map[String,MapDef]() // declared global maps
  override def genLMS(s0:System):String = {
    maps=s0.maps.map(m=>(m.name,m)).toMap

    //TODO: this should be replaced by a specific traversal for completing the slice information
    // s0.triggers.map(super.genTrigger)
    val tsResBlks = s0.triggers.map(genTriggerLMS) // triggers (need to be generated before maps)
    val ts = tsResBlks.map{ case (s,b) =>
      "def "+s+" {\n"+ddbt.Utils.ind(impl.emit(b))+"\n}"
    }.mkString("\n\n")
    val ms = s0.maps.map(genMap).mkString("\n") // maps
    val ds = if(M3MapCommons.isInliningHigherThanNone) M3MapCommons.generateAllEntryClasses else ""
    val r=ms+"\n"+ts+"\n"+ds
    maps=Map()
    M3MapCommons.clear
    r
  }
}
