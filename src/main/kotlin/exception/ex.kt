package exception

class ServiceException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor() : super()

    override fun fillInStackTrace() = this
}