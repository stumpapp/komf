package snd.komf.mediaserver.stump

class StumpResourceNotFoundException(message: String = "Resource not found") : Exception(message)
class StumpAuthenticationException(message: String = "Authentication failed") : Exception(message)
class StumpGraphQLException(message: String, val errors: List<String> = emptyList()) : Exception(message)