package ui

import brainfuck.{BFManaged, BFToCPP, BFToScala, FlufflePuff, Ook}
import common.{EsoObj, Interpreter, Translator, Transpiler}
import deadfish.Deadfish
import emmental.Emmental
import fractran.{FracTran, FracTranpp}
import funge.{Befunge93, Befunge98}
import pdoubleprime.PDP
import scala_run.ScalaRun
import slashes.Slashes
import thue.Thue
import whitespace.{WSAssembly, WhiteSpace, WhiteSpaceToScala}
import wierd.Wierd

import scala.collection.immutable

object EsoDefaults extends EsoObj{
  val defPointer: String = "Eso> "
  val defWelcome: String =
    """|Welcome to Eso, the functional esoteric language interpreter!
       |Type "help" for a list of commands.""".stripMargin
  
  val defBindFile: String = "userBindings.txt"
  val defInterpVec: Vector[Interpreter] = Vector[Interpreter](BFManaged, WhiteSpace, FracTran, FracTranpp, Thue, PDP, Slashes, Deadfish, Emmental, Befunge93, Befunge98, Wierd, ScalaRun)
  val defTransVec: Vector[Translator] = Vector[Translator](FlufflePuff, Ook, WSAssembly)
  val defGenVec: Vector[Transpiler] = Vector[Transpiler](BFToScala, BFToCPP, WhiteSpaceToScala)
  val defBoolVec: Vector[(String, Boolean, String)] = Vector[(String, Boolean, String)](
    ("log", false, "toggle detailed console logging"),
    ("dyn", false, "resize tape as needed for BF interpreter to eliminate memory limitations"),
    ("fPtr", true, "toggle whether output for P'' programs starts at the read head going right or at the end of the tape going left"),
    ("sHead", true, "toggle whether the read head starts at the beginning of the initial tape or the right end of the tape for P''"),
    ("pNull", false, "toggle whether to print the null/empty character in the output of P'' programs"),
    ("indent", false, "toggle whether or not to neatly indent generated Scala code"),
    ("dfChar", true, "toggle whether or not to print Deadfish output as char values"),
    ("bfDiv", true, "toggle whether or not divison by 0 evaluates to 0 in Befunge-98 (not yet implemented)"),
    ("bfRetCode", false, "toggle whether or not the Befunge-98 return code is displayed"))
  val defNumVec: Vector[(String, Int, String)] = Vector[(String, Int, String)](
    ("bfOpt", 2, "BrainFuck interpreter selection: 0=base, 1=optimized, 2=compiled"),
    ("init", 40000, "initial tape size for BrainFuck interpreter"),
    ("olen", -1, "maximum output length, useful for non-terminating programs, -1=infinite"),
    ("methSize", 1000, "maximum number of blocks in a generated method"))
  val defDesc: immutable.HashMap[String, String] = mkMap((defBoolVec ++ defNumVec).map{case (id, _, dc) => (id, dc)})
  
  val defInterpMap: immutable.HashMap[String, Interpreter] = mkMap(defInterpVec map (i => (i.name, i)))
  val defTransMap: immutable.HashMap[(String, String), Translator] = mkMap(defTransVec map (t => (t.id, t)))
  val defGenMap: immutable.HashMap[(String, String), Transpiler] = mkMap(defGenVec map (g => (g.id, g)))
}
