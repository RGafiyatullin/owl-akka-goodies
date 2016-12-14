package com.github.rgafiyatullin.owl_akka_goodies.plugin_support.wiring

import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.wiring.Priority.SimplePriority

sealed trait Priority extends Ordered[Priority] {
  import Priority.{First, Early, Normal, Late, Last, Before, After}

  def base: SimplePriority
  def reduce: Priority

  override def compare(that: Priority): Int = {
    val thisR = reduce
    val thatR = that.reduce

    if (thisR == thatR) 0
    else
      (thisR.base, thatR.base) match {
        case (sameThis, sameThat) if sameThis == sameThat =>
          (thisR, thatR) match {
            case (Before(_), _: SimplePriority) => -1
            case (After(_), _: SimplePriority) => 1
            case (_: SimplePriority, Before(_)) => 1
            case (_: SimplePriority, After(_)) => -1

            case (Before(thisSub), Before(thatSub)) => thisSub compare thatSub
            case (After(thisSub), After(thatSub)) => thisSub compare thatSub

            case (Before(_), After(_)) => -1
            case (After(_), Before(_)) => 1

            case (thisSimplePriority: SimplePriority, thatSimplePriority: SimplePriority) =>
              assert(thisSimplePriority == thatSimplePriority)
              0
          }

        case (First, _) => -1
        case (Early, First) => 1
        case (Early, _) => -1
        case (Normal, First) => 1
        case (Normal, Early) => 1
        case (Normal, _) => -1
        case (Late, First) => 1
        case (Late, Early) => 1
        case (Late, Normal) => 1
        case (Late, _) => -1
        case (Last, _) => 1
      }
  }
}

object Priority {
  sealed trait SimplePriority extends Priority {
    override def base: SimplePriority = this
    override def reduce: Priority = this
  }

  case object First extends SimplePriority
  case object Early extends SimplePriority
  case object Normal extends SimplePriority
  case object Late extends SimplePriority
  case object Last extends SimplePriority

  sealed trait CompoundPriority extends Priority

  final case class Before(p: Priority) extends CompoundPriority {
    override def base: SimplePriority = p.base

    override def reduce: Priority = p match {
      case After(pp) => pp.reduce
      case _ => Before(p.reduce)
    }
  }
  final case class After(p: Priority) extends CompoundPriority {
    override def base = p.base

    override def reduce: Priority = p match {
      case Before(pp) => pp.reduce
      case _ => After(p.reduce)
    }
  }
}
