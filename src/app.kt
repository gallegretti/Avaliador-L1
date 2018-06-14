open class Expression

// Values

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

// Conditional

class ExpIf(val e1: Expression, val e2: Expression, val e3: Expression) : Expression()

// Arithmetic Ops

open class ExpArithmeticOp(val e1: Expression, val e2: Expression) : Expression()
class ExpOpAdd(e1: Expression, e2: Expression) : ExpArithmeticOp(e1, e2)
class ExpOpSub(e1: Expression, e2: Expression) : ExpArithmeticOp(e1, e2)
class ExpOpMult(e1: Expression, e2: Expression) : ExpArithmeticOp(e1, e2)

// Logic Ops

open class ExpLogicOp(val e1: Expression, val e2: Expression) : Expression()
class ExpGreater(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)
class ExpGreaterOrEqual(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)
class ExpEqual(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)
class ExpNotEqual(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)
class ExpLessOrEqual(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)
class ExpLess(e1: Expression, e2: Expression) : ExpLogicOp(e1, e2)

// Functions

class ExpApply(val e1: Expression, val e2: Expression) : Expression()
class ExpLambda(val function: (Expression) -> Expression) : Expression()
class ExpFunction(val function: Expression, val scope: (Expression) -> Expression) : Expression()

// Lists

class ExpNil() : Expression() {
    override fun toString(): String {
        return "nil"
    }
}

class ExpList(val list: MutableList<Expression>) : Expression()
class ExpConcat(val e1: Expression, val e2: Expression) : Expression()
class ExpHead(val e1: Expression) : Expression()
class ExpTail(val e1: Expression) : Expression()
class ExpIsEmpty(val e1: Expression) : Expression()

// Exceptions

class ExpTry(val _try: Expression, val with: Expression) : Expression()
class ExpRaise() : Expression() {
    override fun toString(): String {
        return "raise"
    }
}


fun ExpEval(exp: Expression): Expression {
    return when (exp) {

    // Functions
        is ExpLambda -> exp
        is ExpApply -> {
            val lambda = ExpEval(exp.e1)
            if (lambda is ExpLambda) {
                val functionArgument = ExpEval(exp.e2)
                return ExpEval(lambda.function(functionArgument))
            } else {
                throw Exception("The operation 'Apply' was performed on a non-lambda expression")
            }
        }
        is ExpFunction -> {
            val function = ExpEval(exp.function)
            return ExpEval(exp.scope(function))
        }


    // Exceptions
        is ExpTry -> {
            val block = ExpEval(exp._try)
            return when (block) {
                is ExpRaise -> ExpEval(exp.with)
                else -> block
            }
        }
        is ExpRaise -> exp


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
                rhs.list.add(0, lhs)
                return ExpList(rhs.list)
            } else {
                throw Exception("The operation 'Concat' was performed on a non-list expression")
            }
        }
        is ExpHead -> {
            val l = ExpEval(exp.e1)
            when (l) {
                is ExpList -> return if (l.list.isNotEmpty()) {
                    ExpEval(l.list[0])
                } else {
                    ExpRaise()
                }
                is ExpRaise -> ExpRaise()
                else -> throw Exception("The operation 'Head' was performed on a non-list expression")
            }

        }
        is ExpTail -> {
            val l = ExpEval(exp.e1)
            when (l) {
                is ExpList -> return if (l.list.isNotEmpty()) {
                    ExpList(l.list.drop(1).toMutableList())
                } else {
                    ExpRaise()
                }
                is ExpRaise -> ExpRaise()
                else -> throw Exception("The operation 'Tail' was performed on a non-list expression")
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
    } else if (lhs is ExpRaise || rhs is ExpRaise) {
        return ExpRaise()
    } else {
        throw Exception("The operation '$name' was performed on non-numeric expressions")
    }
}

fun auxDoLogicOp(exp: ExpLogicOp, f: (Int, Int) -> Boolean, name: String): Expression {
    val lhs = ExpEval(exp.e1)
    val rhs = ExpEval(exp.e2)
    if (lhs is ExpNumber && rhs is ExpNumber) {
        return ExpBool(f(lhs.n, rhs.n))
    } else if (lhs is ExpRaise || rhs is ExpRaise) {
        return ExpRaise()
    } else {
        throw Exception("The operation '$name' was performed on non-numeric expressions")
    }
}


fun main(args: Array<String>) {

    // Examples:
    // (10+10) == 20
    println(ExpEval(
            ExpEqual(
                    ExpOpAdd(
                            ExpNumber(10),
                            ExpNumber(10)
                    ),
                    ExpNumber(20)
            )
    ))

    // if (2 < 0) then 42 else 0
    println(ExpEval(
            ExpIf(
                    ExpLess(ExpNumber(2), ExpNumber(0)),
                    ExpNumber(42),
                    ExpNumber(0)
            )
    ))

    // isEmpty([nil])
    println(ExpEval(
            ExpIsEmpty(ExpNil())
    ))

    // isEmpty([0::nil])
    println(ExpEval(
            ExpIsEmpty(
                    ExpConcat(
                            ExpNumber(0),
                            ExpNil()
                    )
            )
    ))

    // hd(tl([0::(1::nil)]))
    println(ExpEval(
            ExpHead(
                    ExpTail(
                            ExpConcat(ExpNumber(0), ExpConcat(ExpNumber(1), ExpNil()))
                    )
            )
    ))

    // try raise with true
    println(ExpEval(
            ExpTry(ExpRaise(), ExpBool(true))
    ))

    // try (if (true) then (raise) else (false)) with false
    println(ExpEval(
            ExpTry(
                    ExpIf(
                            ExpBool(true),
                            ExpRaise(),
                            ExpBool(false)
                    ),
                    ExpBool(true)
            )
    ))

    // try (if(true) then (hd(nil)) else nil) with 0
    println(ExpEval(
            ExpTry(
                    ExpIf(ExpBool(true), ExpHead(ExpNil()), ExpNil()),
                    ExpNumber(0)
            )
    ))

    // 1 + hd(hd(1::nil))
    println(ExpEval(
            ExpOpAdd(ExpNumber(1), ExpTail(ExpTail(ExpConcat(ExpNumber(1), ExpNil()))))
    ))

    // (fn x : Int -> x + 1)(10)
    println(ExpEval(
            ExpApply(
                    ExpLambda({ x ->
                        ExpOpAdd(
                                ExpNumber(1),
                                x
                        )
                    }), ExpNumber(1))
    ))

    // let inc = (fn x -> x + 1) in inc(1)
    println(ExpEval(
            ExpFunction(
                    // let function
                    ExpLambda({ x ->
                        ExpOpAdd(
                                ExpNumber(1),
                                x
                        )
                    }),
                    // 'inc' in ...
                    ({ inc ->
                        ExpApply(inc, ExpNumber(1))
                    }))
    ))
}