package me.tatarka.ipromise;

/**
 * A {@code Deferred} is the producer end of a {@link Promise}. An asynchronous method creates a {@code Deferred} and
 * returns {@link Deferred#promise()}, then calls {@link Deferred#resolve(Object)} or {@link Deferred#reject(Exception)}
 * at a later time.
 * <pre>
 * <code>
 * public Promise{@code <Result, Error>} async() {
 *     final Deferred{@code <Result, Error>} deferred = new Deferred{@code <Result, Error>}();
 *         doAsync(new Callback() {
 *             {@literal @}Override
 *             public void onResult(Result result) {
 *                 deferred.deliver(result);
 *             }
 *         });
 *     return deferred.promise();
 * }
 * </code>
 * </pre>
 *
 * @param <T> the type of a successful result
 * @param <E> the type of an error result
 */
public class Deferred<T, E extends Exception> {
   private Promise<T, E> promise;

   public Deferred() {
      promise = new Promise<T, E>();
   }

   public Deferred(CancelToken cancelToken) {
      promise = new Promise<T, E>(cancelToken);
   }

   public Promise<T, E> promise() {
      return promise;
   }

   /**
    * Delivers a successful result to all listeners of the {@code Promise}. Only one result can be delivered. If the
    * promise has already been canceled, the result will not be stored and listeners will not be notified.
    *
    * @param success the successful result to reject.
    * @throws Promise.AlreadyDeliveredException
    *          throws if a result has already been delivered.
    */
   public synchronized void resolve(T success) {
      promise.deliver(Result.<T, E>success(success));
   }

   /**
    * Delivers an error result to all listeners of the {@code Promise}. Only one result can be delivered. If the
    * promise has already been canceled, the result will not be stored and listeners will not be notified.
    *
    * @param error the error result to reject.
    * @throws Promise.AlreadyDeliveredException
    *          throws if a result has already been delivered.
    */
   public synchronized void reject(E error) {
      promise.deliver(Result.<T, E>error(error));
   }
}
