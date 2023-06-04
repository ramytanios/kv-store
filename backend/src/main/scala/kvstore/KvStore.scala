package kvstore

import cats.effect._
import cats.effect.implicits._
import cats.implicits._

trait KvStore[F[_], K, V] {

  /** insert the given key value pair in the store. if key already exists,
    * the new value will overwrite the old one.
    */
  def insert(key: K, value: V): F[Unit]

  /** get a value based on a key from store */
  def get(key: K): F[V]

  /** remove a key value pair from the store */
  def remove(key: K): F[Unit]

  /** list of all key-value pairs */
  def entries: F[List[(K, V)]]

  /** number of keys */
  def size: F[Int]
}

object KvStore {

  class KvStoreMissingKeyException(msg: String) extends RuntimeException(msg)
  class KvStoreMaxSizeReachedException(msg: String)
      extends RuntimeException(msg)

  /** implementation based on an in-memory `scala.immutable.Map` of predefined
    * maximum size.
    */
  def make[F[_], K, V](maxSize: Int)(implicit
      F: Concurrent[F]
  ): Resource[F, KvStore[F, K, V]] =
    for {
      storeR <- F.ref(Map.empty[K, V]).toResource
      sizeR <- F.ref(0).toResource
    } yield new KvStore[F, K, V] {

      override def insert(key: K, value: V): F[Unit] =
        F.ifF(sizeR.get.map(_ < maxSize))(
          storeR.update(_.updated(key, value)) *> sizeR.update(_ + 1),
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
        storeR.update(_.removed(key)) *> sizeR.update(_ - 1)

      override def entries: F[List[(K, V)]] = storeR.get.map(_.toList)

      override def size: F[Int] = sizeR.get

    }

}
