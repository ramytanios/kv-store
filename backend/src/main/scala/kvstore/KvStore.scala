package kvstore

import cats.effect._
import cats.effect.implicits._
import cats.implicits._

trait KvStore[F[_], K, V] {

  /** update the store with the given key value pair */
  def put(key: K, value: V): F[Unit]

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

  /** implementation based on a local `Map` */
  def make[F[_], K, V](implicit
      F: Concurrent[F]
  ): Resource[F, KvStore[F, K, V]] =
    for {
      store <- F.ref(Map.empty[K, V]).toResource
    } yield new KvStore[F, K, V] {

      override def put(key: K, value: V): F[Unit] =
        store.update(_.updated(key, value))

      override def get(key: K): F[V] =
        store.get
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
        store.update(_.removed(key))

      override def entries: F[List[(K, V)]] = store.get.map(_.toList)

      override def size: F[Int] = store.get.map(_.size)

    }

}
