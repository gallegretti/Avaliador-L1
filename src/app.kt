
open class Expression

class ExpNumber(val n: Int) : Expression() {
    override fun toString(): String {
        return n.toString()
    }
}

class ExpBool(val b: Boolean) : Expression() {
    override fun toString(): String {
        return b.toString()
    }
}

class ExpIf(val e1: Expression, val e2: Expression, val e3: Expression) : Expression()

open class ExpArithmeticOp(val e1: Expression, val e2: Expression) : Expression()
class ExpOpAdd(e1: Expression, e2: Expression) : ExpArithmeticOp(e1, e2)
class ExpOpSub(e1: Expression, e2: Expression) : ExpArithmeticOp(e1, e2)
class ExpOpMult(e1: Expression, e2: Expression) : ExpArithmeticOp(e1, e2)

open class ExpLogicOp(val e1: Expression, val e2: Expression) : Expression()
class ExpGreater(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)
class ExpGreaterOrEqual(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)
class ExpEqual(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)
class ExpNotEqual(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)
class ExpLessOrEqual(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)
class ExpLess(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)

// Lists

class ExpNil() : Expression()
class ExpList(val list: MutableList<Expression>) : Expression()
class ExpConcat(val e1: Expression, val e2: Expression) : Expression()
class ExpHead(val e1: Expression) : Expression()
class ExpTail(val e1: Expression) : Expression()
class ExpIsEmpty(val e1: Expression) : Expression()


fun ExpEval(exp: Expression): Expression {
    return when (exp) {

    // Is value
        is ExpBool -> exp
        is ExpNumber -> exp
    // If
        is ExpIf -> {
            val condition = ExpEval(exp.e1)
            when (condition) {
                is ExpBool -> return if (condition.b) ExpEval(exp.e2) else ExpEval(exp.e3)
                else -> throw Exception("Condition is not a boolean value")
            }
        }
    // Arithmetic
        is ExpOpAdd -> {
            auxDoArithmeticOp(exp, Int::plus, "+")
        }
        is ExpOpSub -> {
            auxDoArithmeticOp(exp, Int::minus, "-")
        }
        is ExpOpMult -> {
            auxDoArithmeticOp(exp, Int::times, "*")
        }
    // Logic
        is ExpGreater -> {
            auxDoLogicOp(exp, { a, b -> a > b }, ">")
        }
        is ExpGreaterOrEqual -> {
            auxDoLogicOp(exp, { a, b -> a >= b }, ">=")
        }
        is ExpEqual -> {
            auxDoLogicOp(exp, { a, b -> a == b }, "==")
        }
        is ExpNotEqual -> {
            auxDoLogicOp(exp, { a, b -> a != b }, "!=")
        }
        is ExpLessOrEqual -> {
            auxDoLogicOp(exp, { a, b -> a <= b }, "<=")
        }
        is ExpLess -> {
            auxDoLogicOp(exp, { a, b -> a < b }, "<")
        }

    // Lists
        is ExpNil -> ExpList(arrayListOf())
        is ExpList -> exp
        is ExpConcat -> {
            val lhs = ExpEval(exp.e1)
            val rhs = ExpEval(exp.e2)
            if (rhs is ExpList) {
                rhs.list.add(lhs)
                return ExpList(rhs.list)
            } else {
                throw Exception("The operation 'Concat' was performed on a non-list expression")
            }
        }
        is ExpHead -> {
            val l = ExpEval(exp.e1)
            if (l is ExpList) {
                if (l.list.isNotEmpty()) {
                    return ExpEval(l.list[0])
                } else {
                    throw Exception("The operation 'Head' was performed on an empty list")
                }
            } else {
                throw Exception("The operation 'Head' was performed on a non-list expression")
            }
        }
        is ExpTail -> {
            val l = ExpEval(exp.e1)
            if (l is ExpList) {
                if (l.list.isNotEmpty()) {
                    return ExpList(l.list.drop(0).toMutableList())
                } else {
                    throw Exception("The operation 'Tail' was performed on an empty list")
                }
            } else {
                throw Exception("The operation 'Tail' was performed on a non-list expression")
            }
        }
        is ExpIsEmpty -> {
            val l = ExpEval(exp.e1)
            if (l is ExpList) {
                return ExpBool(l.list.isEmpty())
            } else {
                throw Exception("The operation 'IsEmpty' was performed on a non-list expression")
            }
        }

        else -> throw Exception("Unimplemented expression type ${exp.javaClass.name}")
    }
}

fun auxDoArithmeticOp(exp: ExpArithmeticOp, f: (Int, Int) -> Int, name: String): Expression {
    val lhs = ExpEval(exp.e1)
    val rhs = ExpEval(exp.e2)
    if (lhs is ExpNumber && rhs is ExpNumber) {
        return ExpNumber(f(lhs.n, rhs.n))
    } else {
        throw Exception("The operation '$name' was performed on non-numeric expressions")
    }
}

fun auxDoLogicOp(exp: ExpLogicOp, f: (Int, Int) -> Boolean, name: String): Expression {
    val lhs = ExpEval(exp.e1)
    val rhs = ExpEval(exp.e2)
    if (lhs is ExpNumber && rhs is ExpNumber) {
        return ExpBool(f(lhs.n, rhs.n))
    } else {
        throw Exception("The operation '$name' was performed on non-numeric expressions")
    }
}


fun main(args: Array<String>) {


    // Examples:

    // (10+10) == 20
    println(
            ExpEval(
                    ExpEqual(
                            ExpOpAdd(
                                    ExpNumber(10),
                                    ExpNumber(10)
                            ),
                            ExpNumber(20)
                    )
            )
    )

    // if (2 < 0) then 42 else 0
    println(
            ExpEval(
                    ExpIf(
                            ExpLess(ExpNumber(2), ExpNumber(0)),
                            ExpNumber(42),
                            ExpNumber(0)
                    )
            )
    )

    // isEmpty([nil])
    println(
            ExpEval(
                    ExpIsEmpty(ExpNil())
            )
    )

    // isEmpty([0::nil])
    println(
            ExpEval(
                    ExpIsEmpty(
                            ExpConcat(
                                    ExpNumber(0),
                                    ExpNil()
                            )
                    )
            )
    )

    // hd(tl([0::(1::nil)]))
    println(
            ExpEval(
                    ExpHead(
                            ExpTail(
                                    ExpConcat(ExpNumber(0), ExpConcat(ExpNumber(1), ExpNil()))
                            )
                    )
            )
    )

}