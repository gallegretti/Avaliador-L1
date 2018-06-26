// Types

open class Type()

class TyInt : Type()
class TyBool : Type()
class TyFn(val input: Type, val output: Type)


open class Expression(var environment: MutableMap<String, Expression> = mutableMapOf())

// Values

data class ExpNumber(val n: Int) : Expression() {
    override fun toString(): String {
        return n.toString()
    }
}

data class ExpBool(val b: Boolean) : Expression() {
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
class ExpVar(val variable: String) : Expression()

class ExpApply(val e1: Expression, val e2: Expression) : Expression()
class ExpLambda(val parameterName: String, val body: Expression) : Expression()
class ExpFunction(val functionName: String, val parameterName: String, val body: Expression, val scope: Expression) : Expression()
class ExpFunctionRec(val functionName: String, val parameterName: String, val body: Expression, val scope: Expression) : Expression()
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
data class ExpRaise(val dummy: Unit? = null) : Expression() {
    override fun toString(): String {
        return "raise"
    }
}


fun ExpEval(exp: Expression): Expression {
    return when (exp) {
        is ExpVar -> {
            return exp.environment[exp.variable] ?: ExpRaise()
        }
    // Functions
        is ExpLambda -> exp
        is ExpApply -> {
            exp.e1.environment = exp.environment
            exp.e2.environment = exp.environment
            val function = ExpEval(exp.e1)
            if (function is ExpFunctionRec) {
                function.body.environment = exp.environment
                function.environment[function.parameterName] = ExpEval(exp.e2)
                return ExpEval(function.body)
            }
            if (function is ExpLambda) {
                function.body.environment = exp.environment
                function.environment[function.parameterName] = ExpEval(exp.e2)
                return ExpEval(function.body)
            }
            if (function is ExpFunction) {
                function.body.environment = exp.environment
                function.body.environment[function.parameterName] = ExpEval(exp.e2)
                return ExpEval(function.body)
            }

            return ExpRaise()

        }
        is ExpFunction -> {
            exp.scope.environment = exp.environment
            exp.scope.environment[exp.functionName] = exp
            return ExpEval(exp.scope)
        }
        is ExpFunctionRec -> {
            exp.scope.environment = exp.environment
            exp.scope.environment[exp.functionName] = exp
            return ExpEval(exp.scope)
        }

    // Exceptions
        is ExpTry -> {
            exp._try.environment = exp.environment
            exp.with.environment = exp.environment
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
            exp.e1.environment = exp.environment
            exp.e2.environment = exp.environment
            exp.e3.environment = exp.environment
            val condition = ExpEval(exp.e1)
            return when (condition) {
                is ExpBool -> if (condition.b) ExpEval(exp.e2) else ExpEval(exp.e3)
                is ExpRaise -> ExpRaise()
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
            exp.e1.environment = exp.environment
            exp.e2.environment = exp.environment
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
            exp.e1.environment = exp.environment
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
            exp.e1.environment = exp.environment
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
            exp.e1.environment = exp.environment
            val l = ExpEval(exp.e1)
            when (l) {
                is ExpList -> return ExpBool(l.list.isEmpty())
                is ExpRaise -> l
                else -> throw Exception("The operation 'IsEmpty' was performed on a non-list expression")
            }
        }

        else -> throw Exception("Unimplemented expression type ${exp.javaClass.name}")
    }
}

fun auxDoArithmeticOp(exp: ExpArithmeticOp, f: (Int, Int) -> Int, name: String): Expression {
    exp.e1.environment = exp.environment
    exp.e2.environment = exp.environment
    val lhs = ExpEval(exp.e1)
    val rhs = ExpEval(exp.e2)
    return if (lhs is ExpNumber && rhs is ExpNumber) {
        ExpNumber(f(lhs.n, rhs.n))
    } else if (lhs is ExpRaise || rhs is ExpRaise) {
        ExpRaise()
    } else {
        throw Exception("The operation '$name' was performed on non-numeric expressions")
    }
}

fun auxDoLogicOp(exp: ExpLogicOp, f: (Int, Int) -> Boolean, name: String): Expression {
    exp.e1.environment = exp.environment
    exp.e2.environment = exp.environment
    val lhs = ExpEval(exp.e1)
    val rhs = ExpEval(exp.e2)
    return if (lhs is ExpNumber && rhs is ExpNumber) {
        ExpBool(f(lhs.n, rhs.n))
    } else if (lhs is ExpRaise || rhs is ExpRaise) {
        ExpRaise()
    } else {
        throw Exception("The operation '$name' was performed on non-numeric expressions")
    }
}

fun assert(expression: Expression, expected: Expression) {
    val evaluated = ExpEval(expression)
    if (evaluated != expected) {
        println("Test failed, expected $expected but found $evaluated")
    } else {
        println("Test succeeded")
    }
}

fun main(args: Array<String>) {

    // Examples:
    // (10+10) == 20
    assert(
            ExpEqual(
                    ExpOpAdd(
                            ExpNumber(10),
                            ExpNumber(10)
                    ),
                    ExpNumber(20)
            ),
            ExpBool(true)
    )

    // if (2 < 0) then 42 else 0
    assert(
            ExpIf(
                    ExpLess(ExpNumber(2), ExpNumber(0)),
                    ExpNumber(42),
                    ExpNumber(0)
            ),
            ExpNumber(0)
    )

    // isEmpty([nil])
    assert(
            ExpIsEmpty(ExpNil()), ExpBool(true)
    )

    // isEmpty([0::nil])
    assert(
            ExpIsEmpty(
                    ExpConcat(
                            ExpNumber(0),
                            ExpNil()
                    )
            ), ExpBool(false)
    )

    // hd(tl([0::(1::nil)]))
    assert(
            ExpHead(
                    ExpTail(
                            ExpConcat(ExpNumber(0), ExpConcat(ExpNumber(1), ExpNil()))
                    )
            ), ExpNumber(1)
    )

    // try raise with true
    assert(
            ExpTry(ExpRaise(), ExpBool(true)), ExpBool(true)
    )

    // try (if (true) then (raise) else (false)) with false
    assert(
            ExpTry(
                    ExpIf(
                            ExpBool(true),
                            ExpRaise(),
                            ExpBool(false)
                    ),
                    ExpBool(false)
            ), ExpBool(false)
    )

    // try (if(true) then (hd(nil)) else nil) with 0
    assert(
            ExpTry(
                    ExpIf(ExpBool(true), ExpHead(ExpNil()), ExpNil()),
                    ExpNumber(0)
            ), ExpNumber(0)
    )

    // 1 + hd(hd(1::nil))
    assert(
            ExpOpAdd(ExpNumber(1), ExpTail(ExpTail(ExpConcat(ExpNumber(1), ExpNil())))),
            ExpRaise()
    )

    // (fn x : Int -> x + 1)(10)
    assert(
            ExpApply(
                    ExpLambda("x",
                            ExpOpAdd(
                                    ExpNumber(1),
                                    ExpVar("x")
                            )), ExpNumber(10)),
            ExpNumber(11))

    // let inc = (fn x -> x + 1) in inc(1)
    assert(
            ExpFunction("inc", "x", ExpOpAdd(
                    ExpNumber(1),
                    ExpVar("x")
            ),
                    ExpApply(ExpVar("inc"), ExpNumber(1))
            ), ExpNumber(2))


    // let fat = (fn x -> if (x == 0) then 1 else x*fat(x-1) in fat(4)
    assert(
            ExpFunctionRec("fat", "x", ExpIf(ExpEqual(ExpVar("x"), ExpNumber(0)),
                    ExpNumber(1),
                    ExpOpMult(ExpVar("x"), ExpApply(ExpVar("fat"), ExpOpSub(ExpVar("x"), ExpNumber(1))))), ExpApply(ExpVar("fat"), ExpNumber(4))
            ), ExpNumber(24)
    )
}