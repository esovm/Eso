package common

import scala.collection.{immutable, mutable}
import scala.util.{Failure, Success, Try}

trait EsoObj {
  def tryAll[T](thing: => T): Try[T] = try{Success(thing)} catch{case e: Throwable => Failure(e)}
  
  def mkMut[A, B](immut: immutable.HashMap[A, B]): mutable.HashMap[A, B] = mutable.HashMap.from(immut)
  def mkImmut[A, B](mut: mutable.HashMap[A, B]): immutable.HashMap[A, B] = immutable.HashMap.from(mut)
  def mkMap(ks: Vector[String], vs: Vector[String]): immutable.HashMap[String, String] = mkMap(ks.zip(vs))
  def mkMap[A, B](vec: Vector[(A, B)]): immutable.HashMap[A, B] = {
    val builder = immutable.HashMap.newBuilder[A, B]
    builder ++= vec
    builder.result
  }
}
