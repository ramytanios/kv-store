package kvstore

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import fs2.concurrent._

trait KvStore[F[_], K, V] {

  /** insert the given key value pair in the store. if key already exists, the
    * new value will overwrite the old one.
    */
  def insert(key: K, value: V): F[Unit]

  /** get a value based on a key from store */
  def get(key: K): F[V]

  /** remove a key value pair from the store */
  def remove(key: K): F[Unit]

  /** list of all key-value pairs */
  def entries: F[List[(K, V)]]

  /** clear the store */
  def clear: F[Unit]

  /** number of keys */
  def size: Signal[F, Int]
}

object KvStore {

  class KvStoreMissingKeyException(msg: String) extends RuntimeException(msg)
  class KvStoreMaxSizeReachedException(msg: String)
      extends RuntimeException(msg)

  /** Implementation based on an in-memory `scala.immutable.Map` of predefined
    * maximum size. The store emits update events when its updated (key value
    * pair added, key value pair removed, etc ...)
    */
  def apply[F[_], K, V](
      maxSize: Int,
      updateEventF: F[Unit]
  )(implicit
      F: Concurrent[F]
  ): Resource[F, KvStore[F, K, V]] =
    for {
      storeR <- fs2.concurrent.SignallingRef(Map.empty[K, V]).toResource
    } yield new KvStore[F, K, V] {

      override def insert(key: K, value: V): F[Unit] =
        F.ifM(storeR.get.map(_.size < maxSize))(
          storeR.update(_.updated(key, value)).flatTap(_ => updateEventF),
          F.raiseError(
            new KvStoreMaxSizeReachedException(s"Max size $maxSize reached.")
          )
        )

      override def get(key: K): F[V] =
        storeR.get
          .map(_.get(key))
          .flatMap { valueOpt =>
            F.fromOption(
              valueOpt,
              new KvStoreMissingKeyException(
                s"Key $key does not exist in the store."
              )
            )
          }

      override def remove(key: K): F[Unit] =
        storeR.update(_.removed(key)).flatTap(_ => updateEventF)

      override def entries: F[List[(K, V)]] = storeR.get.map(_.toList)

      override def clear: F[Unit] =
        storeR.set(Map.empty[K, V]).flatTap(_ => updateEventF)

      override def size: Signal[F, Int] = storeR.map(_.size)

    }

}
