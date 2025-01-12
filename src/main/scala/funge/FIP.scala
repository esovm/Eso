package funge

import java.util.Calendar

import common.Vec2D

import scala.util.Random
import spire.implicits._

import scala.collection.immutable

trait FIPRet{
  def step: FIPRet
}
case class FIPCont(prog: BF98Prog, inp: Seq[Char], fip: FIP) extends FIPRet{
  def step: FIPCont = FIPCont(prog, inp, fip.step(prog))
}
case class FIPSplit(prog: BF98Prog, inp: Seq[Char], fip1: FIP, fip2: FIP) extends FIPRet{
  def step: FIPSplit = FIPSplit(prog: BF98Prog, inp, fip1.step(prog), fip2.step(prog))
}
case class FIPOut(out: String, prog: BF98Prog, inp: Seq[Char], fip: FIP) extends FIPRet{
  def step: FIPOut = FIPOut(out, prog, inp, fip.step(prog))
}
case class FIPHalt(code: Option[Int]) extends FIPRet{
  def step: FIPHalt = this
}

case class FIP(id: Int, ip: Vec2D[Int], dt: Vec2D[Int], so: Vec2D[Int], bs: Boolean, stk: Vector[LazyList[Int]], binds: immutable.HashMap[Char, Vector[(BF98Prog, Seq[Char], FIP) => FIPRet]]){
  val rand = new Random
  def cleared: LazyList[Int] = LazyList.continually(0)
  
  def apply(prog: BF98Prog, inp: Seq[Char]): FIPRet = {
    val npos = prog.getNextInd(ip, dt)
    val op = prog(ip).toChar
    doOp(prog, inp, npos, op)
  }
  
  def step(prog: BF98Prog): FIP = FIP(id, prog.getNextInd(ip, dt), dt, so, bs, stk, binds)
  
  def doOp(prog: BF98Prog, inp: Seq[Char], npos: Vec2D[Int], op: Char): FIPRet = {
    if(bs){
      if(op == '"') FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, dt), dt, so, bs=false, stk, binds))
      else if(op == ' ') FIPCont(prog, inp, FIP(id, prog.skipAll(ip, dt, op.toInt), dt, so, bs, (op.toInt +: TOSS) +: stk.tail, binds))
      else FIPCont(prog, inp, FIP(id, ip + dt, dt, so, bs, (op.toInt +: TOSS) +: stk.tail, binds))
    }else{
      if(op != 'k') exec(prog, inp, npos, op)
      else TOSS match{
        case n +: ns =>
          if(n == 0){
            val npos2 = prog.getNextInd(npos, dt)
            FIPCont(prog, inp, FIP(id, npos2, dt, so, bs, ns +: stk.tail, binds))
          }else{
            prog(npos).toChar match{
              case '#' => TOSS match{
                case n +: ns => FIPCont(prog, inp, FIP(id, prog.getNextInd(ip + (dt*n), dt), dt, so, bs, ns +: stk.tail, binds))
              }
              case op2 => rep(prog, inp, ip, op2, n).step
            }
          }
      }
    }
  }
  
  def rep(prog: BF98Prog, inp: Seq[Char], npos: Vec2D[Int], opChar: Char, num: Int): FIPRet = {
    lazy val opVec = Vector.fill(num)(opChar)
    lazy val res = opVec.foldLeft(("": String, FIP(id, npos, dt, so, bs, TOSS.tail +: stk.tail, binds), prog, inp)){
      case ((str, fip, prg, in), op) =>
        fip.doOp(prg, in, fip.ip, op) match{
          case FIPCont(nPrg, nIn, nFip) => (str, nFip, nPrg, nIn)
          case FIPOut(nStr, nPrg, nIn, nFip) => (str ++ nStr, nFip, nPrg, nIn)
        }
    }
    
    opChar match{
      case '@' | 'q' => exec(prog, inp, npos, opChar)
      case _ => res match{
        case (str, fip, prg, in) =>
          if(str.isEmpty) FIPCont(prg, in, fip)
          else FIPOut(str, prg, in, fip)
      }
    }
  }
  
  def exec(prog: BF98Prog, inp: Seq[Char], npos: Vec2D[Int], op: Char): FIPRet = {
    val ff = 12.toChar
    op match{
      case '!' =>
        val not = if(TOSS.head == 0) 1 else 0
        val nStk = (not +: TOSS.tail) +: stk.tail
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nStk, binds))
      case '\"' => FIPCont(prog, inp, FIP(id, ip + dt, dt, so, bs=true, stk, binds))
      case '#' =>  FIPCont(prog, inp, FIP(id, prog.getNextInd(ip + dt, dt), dt, so, bs, stk, binds))
      case '$' =>
        val nStk = TOSS.tail +: stk.tail
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nStk, binds))
      case '&' =>
        val (num, ninp) = chompNum(inp)
        val nStk = (num +: TOSS) +: stk.tail
        FIPCont(prog, ninp, FIP(id, npos, dt, so, bs, nStk, binds))
      case '\'' =>
        val nStk = (prog(ip + dt) +: TOSS) +: stk.tail
        val npos2 = prog.getNextInd(ip + dt, dt)
        FIPCont(prog, inp, FIP(id, npos2, dt, so, bs, nStk, binds))
      case 's' =>
        val (nProg, nToss) = TOSS match{
          case v +: ns =>
            (prog.updated(ip + dt, v), ns)
        }
        FIPCont(nProg, inp, FIP(id, prog.getNextInd(ip + dt, dt), dt, so, bs, nToss +: stk.tail, binds))
      case '*' =>
        val nToss = TOSS match{
          case b +: a +: ns => (a*b) +: ns
        }
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nToss +: stk.tail, binds))
      case '/' =>
        val nToss = TOSS match{
          case b +: a +: ns => (if(b == 0 && prog.bDiv) 0 else a/b) +: ns
        }
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nToss +: stk.tail, binds))
      case '%' =>
        val nToss = TOSS match{
          case b +: a +: ns => (if(b == 0 && prog.bDiv) 0 else a%b) +: ns
        }
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nToss +: stk.tail, binds))
      case '+' =>
        val nToss = TOSS match{
          case b +: a +: ns => (a + b) +: ns
        }
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nToss +: stk.tail, binds))
      case '-' =>
        val nToss = TOSS match{
          case b +: a +: ns => (a - b) +: ns
        }
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nToss +: stk.tail, binds))
      case '.' => FIPOut(s"${TOSS.head.toString} ", prog, inp, FIP(id, npos, dt, so, bs, TOSS.tail +: stk.tail, binds))
      case ',' => FIPOut(TOSS.head.toChar.toString, prog, inp, FIP(id, npos, dt, so, bs, TOSS.tail +: stk.tail, binds))
      case n if n.isDigit =>
        val nNum = n.asDigit
        val nStk = (nNum +: TOSS) +: stk.tail
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nStk, binds))
      case c if Range('a', 'g').contains(c) => FIPCont(prog, inp, FIP(id, npos, dt, so, bs, ((c.toInt - 87) +: TOSS) +: stk.tail, binds))
      case ':' =>
        val nStk = (TOSS.head +: TOSS) +: stk.tail
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nStk, binds))
      case '<' =>
        val nDt = Vec2D(-1, 0)
        val npos2 = prog.getNextInd(ip, nDt)
        FIPCont(prog, inp, FIP(id, npos2, nDt, so, bs, stk, binds))
      case '>' =>
        val nDt = Vec2D(1, 0)
        val npos2 = prog.getNextInd(ip, nDt)
        FIPCont(prog, inp, FIP(id, npos2, nDt, so, bs, stk, binds))
      case '^' =>
        val nDt = Vec2D(0, -1)
        val npos2 = prog.getNextInd(ip, nDt)
        FIPCont(prog, inp, FIP(id, npos2, nDt, so, bs, stk, binds))
      case 'v' =>
        val nDt = Vec2D(0, 1)
        val npos2 = prog.getNextInd(ip, nDt)
        FIPCont(prog, inp, FIP(id, npos2, nDt, so, bs, stk, binds))
      case '?' =>
        val nDt = rand.nextInt(4) match{
          case 0 => Vec2D(-1, 0)
          case 1 => Vec2D(1, 0)
          case 2 => Vec2D(0, -1)
          case 3 => Vec2D(0, 1)
        }
        val npos2 = prog.getNextInd(ip, nDt)
        FIPCont(prog, inp, FIP(id, npos2, nDt, so, bs, stk, binds))
      case '[' =>
        val nDt = Vec2D(dt.y, -dt.x)
        val npos2 = prog.getNextInd(ip, nDt)
        FIPCont(prog, inp, FIP(id, npos2, nDt, so, bs, stk, binds))
      case ']' =>
        val nDt = Vec2D(-dt.y, dt.x)
        val npos2 = prog.getNextInd(ip, nDt)
        FIPCont(prog, inp, FIP(id, npos2, nDt, so, bs, stk, binds))
      case '@' => FIPHalt(None)
      case 'q' => FIPHalt(Some(TOSS.head))
      case '\\' =>
        val nToss = TOSS match{
          case b +: a +: ns => a +: b +: ns
        }
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nToss +: stk.tail, binds))
      case '_' =>
        val nDt = if(TOSS.head == 0) Vec2D(1, 0) else Vec2D(-1, 0)
        val npos2 = prog.getNextInd(ip, nDt)
        FIPCont(prog, inp, FIP(id, npos2, nDt, so, bs, TOSS.tail +: stk.tail, binds))
      case '|' =>
        val nDt = if(TOSS.head == 0) Vec2D(0, 1) else Vec2D(0, -1)
        val npos2 = prog.getNextInd(ip, nDt)
        FIPCont(prog, inp, FIP(id, npos2, nDt, so, bs, TOSS.tail +: stk.tail, binds))
      case '`' =>
        val nToss = TOSS match{
          case b +: a +: ns =>
            val gt = if(a > b) 1 else 0
            gt +: ns
        }
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nToss +: stk.tail, binds))
      case 'g' =>
        val nToss = TOSS match{
          case y +: x +: ns =>
            val va = Vec2D(x, y)
            val got = prog(so + va)
            got +: ns
        }
        FIPCont(prog, inp, FIP(id, npos, dt, so, bs, nToss +: stk.tail, binds))
      case 'p' =>
        val (nProg, nToss) = TOSS match{
          case y +: x +: v +: ns =>
            val ptr = Vec2D(x, y)
            (prog.updated(so + ptr, v), ns)
        }
        FIPCont(nProg, inp, FIP(id, npos, dt, so, bs, nToss +: stk.tail, binds))
      case 'j' =>
        val (npos2, nToss) = TOSS match{
          case n +: ns => (prog.getNextInd(ip + (dt*n), dt), ns)
        }
        FIPCont(prog, inp, FIP(id, npos2, dt, so, bs, nToss +: stk.tail, binds))
      case 'n' => FIPCont(prog, inp, FIP(id, npos, dt, so, bs, cleared +: stk.tail, binds))
      case 'r' => FIPCont(prog, inp, FIP(id, npos, -dt, so, bs, stk, binds))
      case 't' => FIPSplit(prog, inp, FIP(id, npos, dt, so, bs, stk, binds), FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, stk, binds))
      case 'u' =>
        if(stk.sizeIs < 2) FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, stk, binds))
        else{
          TOSS match{
            case n +: ns =>
              //              //Thread.sleep(5000)
              if(n == 0) FIPCont(prog, inp, FIP(id, npos, dt, so, bs, ns +: stk.tail, binds))
              else if(n > 0) FIPCont(prog, inp, FIP(id, npos, dt, so, bs, (SOSS.take(n).reverse ++ ns) +: SOSS.drop(n) +: stk.drop(2), binds))
              else FIPCont(prog, inp, FIP(id, npos, dt, so, bs, ns.drop(-n) +: (ns.take(-n).reverse ++ SOSS) +: stk.drop(2), binds))
          }
        }
      case 'w' =>
        val (nDt, nToss) = TOSS match{
          case a +: b +: ns =>
            if(a > b) (Vec2D(dt.y, -dt.x), ns)
            else if(a < b) (Vec2D(-dt.y, dt.x), ns)
            else (dt, ns)
        }
        FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, nDt), nDt, so, bs, nToss +: stk.tail, binds))
      case 'x' =>
        val (nDt, nToss) = TOSS match{
          case y +: x +: ns => (Vec2D(x, y), ns)
        }
        FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, nDt), nDt, so, bs, nToss +: stk.tail, binds))
      case 'z' => FIPCont(prog, inp, FIP(id, npos, dt, so, bs, stk, binds))
      case '{' =>
        val nSo = npos
        val nStk = TOSS match{
          case n +: ns =>
            val num = math.max(0, n)
            val rev = math.max(0, -n)
            val nToss = ns.take(num) ++ cleared
            val nSoss = so.y +: so.x +: LazyList.fill(rev)(0) ++: ns.drop(num)
            nToss +: nSoss +: stk.drop(2)
        }
        FIPCont(prog, inp, FIP(id, npos, dt, nSo, bs, nStk, binds))
      case '}' =>
        stk match{
          case (n +: ns) +: (y +: x +: ss) +: tail =>
            val nSo = Vec2D(x, y)
            val nStk = if(n >= 0) (ns.take(n) ++ ss) +: tail else ss.drop(n.abs) +: tail
            FIPCont(prog, inp, FIP(id, npos, dt, nSo, bs, nStk, binds))
          case _ => FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, stk, binds))
        }
      case '~' => FIPCont(prog, inp.tail, FIP(id, npos, dt, so, bs, (inp.head.toInt +: TOSS) +: stk.tail, binds))
      case 'y' =>
        import SysInf._
        val (low, high) = prog.getBounds
        val cal = prog.cal
        val date = cal.get(Calendar.DATE)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val hour = cal.get(Calendar.HOUR)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val ymd = ((year - 1900)*256*256) + (month*256) + date
        val hms = (hour*256*256) + (minute*256) + second
        
        val flags = Vector(tFlg, iFlg, oFlg, eqFlg, bufFlg).zipWithIndex.foldLeft(0: Int){case (s, (f, n)) => s + (f*(2**n))}
        val global = Vector(flags, bytesPerCell, handPrint, version, paradigm, pathSep.toInt, dims)
        val local = Vector(id, 0, ip.y, ip.x, dt.y, dt.x, so.y, so.x)
        val env = Vector(low.y, low.x, high.y, high.x, ymd, hms)
        val stkInf = stk.size +: Vector.fill(stk.size)(-1)
        val args = Vector.fill(4)(0)
        
        val infoStk = (global ++ local ++ env ++ stkInf ++ args).to(LazyList)
        TOSS match{
          case n +: ns =>
            if(n <= 0) FIPCont(prog, inp, FIP(id, npos, dt, so, bs, (infoStk ++ ns) +: stk.tail, binds))
            else{
              val tmpToss = infoStk ++ ns
              val pick = tmpToss(n - 1)
              FIPCont(prog, inp, FIP(id, npos, dt, so, bs, (pick +: ns) +: stk.tail, binds))
            }
        }
      case c if binds.isDefinedAt(c) && binds(c).nonEmpty => binds(c).head(prog, inp, this)
      case '(' =>
        TOSS match{
          case n +: ns =>
            val nam = ns.take(n)
            val id = nam.foldLeft(0){case (ac, n) => (ac*256) + n}
            BF98Lib.get(id) match{
              case Some(fp) =>
                val nBind = fp.binds.foldLeft(binds){
                  case (bs, (k, b)) =>
                    val eb = bs.get(k) match{
                      case Some(v) => v
                      case None => Vector()
                    }
                    bs + ((k, b +: eb))
                }
                FIPCont(prog, inp, FIP(id, npos, dt, so, bs, (1 +: id +:ns.drop(n)) +: stk.tail, nBind))
              case None => FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, ns.drop(n) +: stk.tail, binds))
            }
        }
      case ')' =>
        TOSS match{
          case n +: ns =>
            val nam = ns.take(n)
            val id = nam.foldLeft(0){case (ac, n) => (ac*256) + n}
            BF98Lib.get(id) match{
              case Some(fp) =>
                val nBind = fp.binds.foldLeft(binds){
                  case (bs, (k, _)) => bs.get(k) match{
                    case Some(v) => bs + ((k, v.drop(1)))
                    case None => bs
                  }
                }
                FIPCont(prog, inp, FIP(id, npos, dt, so, bs, ns.drop(n) +: stk.tail, nBind))
              case None => FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, ns.drop(n) +: stk.tail, binds))
            }
        }
      case 'i' => FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, stk, binds)) //Input file (not implemented)
      case 'o' => FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, stk, binds)) //Output file (not implemented)
      case '=' => FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, stk, binds)) //Execute (not implemented)
      case 'h' => FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, stk, binds))
      case 'l' => FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, stk, binds))
      case _ => FIPCont(prog, inp, FIP(id, prog.getNextInd(ip, -dt), -dt, so, bs, stk, binds))
    }
  }
  
  def chomp(inp: Seq[Char]): (String, Seq[Char]) = {
    val (hd, tl) = inp.splitAt(inp.indexOf('\n'))
    (hd.mkString, tl.tail)
  }
  def chompNum(inp: Seq[Char]): (Int, Seq[Char]) = {
    val inp2 = inp.dropWhile(c => !c.isDigit)
    val num = inp.takeWhile(_.isDigit).mkString.toInt
    (num, inp2.dropWhile(_.isDigit))
  }
  
  def TOSS: LazyList[Int] = stk.head
  def SOSS: LazyList[Int] = stk.tail.head
  
  def setID(nid: Int): FIP = FIP(nid, ip, dt, so, bs, stk, binds)
  
  override def toString: String = s"{id=$id, ip=$ip, dt=$dt, so=$so, bs=$bs, stk = [${stk.map(lst => s"[${lst.take(4).mkString(", ")}, ...]").take(2).mkString(" ")}]}"
}

object SysInf{
  val tFlg = 1
  val iFlg = 0
  val oFlg = 0
  val eqFlg = 0
  val bufFlg = 0
  
  val bytesPerCell = 4
  val handPrint = 1165193033 //EsoI encoded as a semantic label
  val version = 1
  val paradigm = 0
  val pathSep = '/'
  val dims = 2
}