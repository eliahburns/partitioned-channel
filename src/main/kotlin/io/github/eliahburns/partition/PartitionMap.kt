package io.github.eliahburns.partition


sealed class MapEither<out E : Throwable, out R> {
    data class Left(val value: Throwable) : MapEither<Throwable, Nothing>()
    data class Right<out R>(val value: R) : MapEither<Nothing, R>()
}

data class MapActionException(
    val element: Any,
    override val cause: Throwable
) : Throwable()


@PublishedApi
internal suspend inline fun <E, R> tryAction(
    element: E,
    crossinline action: suspend (E) -> R
): MapEither<Throwable, R> {
    return try {
        MapEither.Right(action(element))
    } catch (e: Exception) {
        MapEither.Left(
            MapActionException(element = element as Any, cause = e)
        )
    }
}


