package io.stipop.models.response

data class ResponseHeader(val code: String, val status: String, val message: String) {
    fun isSuccess(): Boolean {
        return code == "200" || status == "success"
    }
}
