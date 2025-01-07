package exception

class ServiceException(message: String?, val code: Int = 200) : RuntimeException(message) {

    override fun fillInStackTrace() = this
}