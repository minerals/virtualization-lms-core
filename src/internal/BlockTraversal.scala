package scala.virtualization.lms
package internal

import util.GraphUtil
import java.io.{File, PrintWriter}


trait BlockTraversal extends GraphTraversal {
  val IR: Expressions
  import IR._

  type Block[+T]
  
  def reifyBlock[T: Manifest](x: => Exp[T]): Block[T]

  def compactize(start: Block[Any], local: List[Sym[Any]]): List[Sym[Any]] = { throw new Exception("Method compactize should be overriden.") }

  def getFreeVarBlock(start: Block[Any], local: List[Sym[Any]]): List[Sym[Any]] = Nil

  def getFreeDataBlock[A](start: Block[A]): List[(Sym[Any],Any)] = Nil // TODO: Nil or Exception??

  def getBlockResult[A](s: Block[A]): Exp[A] = getBlockResultFull(s) // = s.res
  def getBlockResultFull[A](s: Block[A]): Exp[A] // = s.res
  
  def traverseBlock[A](block: Block[A]): Unit
  def traverseStm(stm: Stm): Unit
  
  def reset: Unit = ()
}




trait NestedBlockTraversal extends BlockTraversal with NestedGraphTraversal {
  val IR: Expressions with Effects
  import IR._

  // ----- block definition

  type Block[+T] = IR.Block[T]
  def reifyBlock[T: Manifest](x: => Exp[T]): Block[T] = IR.reifyEffects(x)

  override def getBlockResultFull[A](s: Block[A]): Exp[A] = s.res
  
  override def getBlockResult[A](s: Block[A]): Exp[A] = s match {
    case Block(Def(Reify(x, _, _))) => x
    case Block(x) => x
  }

  
  def focusBlock[A](result: Block[Any])(body: => A): A = 
    focusFatBlock(List(result))(body)
    
  def focusFatBlock[A](result: List[Block[Any]])(body: => A): A = 
    focusSubGraph[A](result.map(getBlockResultFull))(body)


  def focusExactScope[A](resultB: Block[Any])(body: Seq[Stm] => A): A = 
    focusExactScopeFat(List(resultB))(body)
  
  def focusExactScopeFat[A](resultB: List[Block[Any]])(body: Seq[Stm] => A): A = 
    focusExactScopeSubGraph[A](resultB.map(getBlockResultFull))(body)
  
  // ---- bound and free vars

  def boundInScope(x: List[Exp[Any]]): List[Sym[Any]] = {
    (x.flatMap(syms)++innerScope.flatMap(t => t.lhs:::boundSyms(t.rhs))).distinct
  }
  
  def usedInScope(y: List[Exp[Any]]): List[Sym[Any]] = {
    (y.flatMap(syms)++innerScope.flatMap(t => syms(t.rhs))).distinct
  }
  
  def readInScope(y: List[Exp[Any]]): List[Sym[Any]] = {
    (y.flatMap(syms)++innerScope.flatMap(t => readSyms(t.rhs))).distinct
  }

  def freeInScope(bound: List[Exp[Any]], used: List[Exp[Any]]): List[Sym[Any]] = {
    // aks: freeInScope used to collect effects that are not true input dependencies. TR, any better solution?
    // i would expect read to be a subset of used, but there are cases where read has symbols not in used (TODO: investigate)
    (usedInScope(used) intersect readInScope(used)) diff boundInScope(bound)
  }

  // TODO: remove
  override def getFreeVarBlock(start: Block[Any], local: List[Sym[Any]]): List[Sym[Any]] = {
    focusBlock(start) {
      freeInScope(local, List(getBlockResultFull(start)))
    }
  }

  override def getFreeDataBlock[A](start: Block[A]): List[(Sym[Any],Any)] = Nil // FIXME: should have generic impl



  // ----- high level api

  def traverseBlock[A](block: Block[A]): Unit = {
    focusBlock(block) {
      traverseBlockFocused(block)
    }
  }

  def traverseBlockFocused[A](block: Block[A]): Unit = {
    focusExactScope(block) { levelScope =>
      traverseStmsInBlock(levelScope)
    }
  }

  def traverseStmsInBlock[A](stms: Seq[Stm]): Unit = {
    stms foreach traverseStm
  }

  def traverseStm(stm: Stm): Unit = { // override this to implement custom traversal
    blocks(stm.rhs) foreach traverseBlock
  }

  // ----- reset

  override def reset { // used anywhere?
    innerScope = null
    //shallow = false
    IR.reset
    super.reset
  }
}

