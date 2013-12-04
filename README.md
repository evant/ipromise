ipromise
======
Writing asynchronous code can quickly become painful, especially when you need to make multiple calls, due to each method taking it's own callback and having it's own idea of error handling and cancellation. Promises alleviate this by providing a uniform interface and by turning callback style into the same return style of synchronous code. You can view the javadoc at http://evant.github.com/ipromise

Usage
-----
You can turn this:

```java
async1(arg, new Callback1() {
    @Override
	public void onResult(MyResult1 result1) {
		async2(result1, new Callback2() {
			@Override
			public void onResult(MyResult2 result2) {
				// Finally! do something with result2.
			}
		}
	}
}
```
Into this:
```java
async1(arg).then(new Chain<MyResult1, Promise<MyResult2>>() {
	@Override
	public Promise<MyResult2> chain(MyResult1 result1) {
		return async2(result2);
	}
}).listen(new Promise.Listener<MyResult2>() {
	@Override
	public void receive(MyResult2 result) {
		// Isn't this nicer?
	}
});
```
See how it reads nice and sequentially instead of further and further indentations? If that doesn't look like a huge improvement to you, lets add some error checking. After all, `async1()` and `async2()` may fail.

```java
async1(arg, new Callback1() {
	@Override
	public void onResult(MyResult1 result1) {
		async2(result1, new Callback2() {
			@Override
			public void onResult(MyResult2 result2) {
				// Finally! do something with result2.
			}
			@Override
			public void onError(Error error) {
				// Take care of the error on the second callback
			}
		}
	}
	@Override
	public void onError(Error error) {
		// Take care of the error on the first callback
	}
}
```
See how the error handling is all over the place? And you would have duplicate code if your error handling in both places was the same.
```java
async1(arg).then(new Result.ChainPromise<MyResult1, MyResult2, Error>() {
	@Override
	public Promise<Result<MyResult2, Error>> success(MyResult1 result1) {
		return async2(result1);
	}
}).listen(new Result.Listener<MyResult2, Error>() {
	@Override
	public void success(MyResult2 result) {
		// Isn't this nicer?
	}
	@Override
	public void error(Error error) {
		// Error checking in one place!
	}
});
```
Much nicer! Now lets say the user decides they don't want to wait anymore and cancels the action.
```java
// The library I'm using doesn't have a way to cancel method calls :(
boolean isCanceled = false;

async1(arg, new Callback1() {
	@Override
	public void onResult(MyResult1 result1) {
		if (isCanceled) return; // Easy to forget one of these
		async2(result1, new Callback2() {
			@Override
			public void onResult(MyResult2 result2) {
				if (isCanceled) return;
				// Finally! do something with result2.
			}
			@Override
			public void onError(Error error) {
				// Take care of the error on the second callback
			}
		}
	}
	@Override
	public void onError(Error error) {
		// Take care of the error on the first callback
	}
}

public void userCancel() {
	isCanceled = true;
}
```
This code is starting to look like a mess! With promises it's so much easier:
```java
Promise<Result<Result2, Error>> promise = async1(arg).then(new Result.ChainPromise<Result1, Result2, Error>() {
	@Override
	public Promise<Result<Result2, Error>> success(Result1 result1) {
		return async2(result1);
	}
}).listen(new Result.Listener<Result2, Error>() {
	@Override
	public void success(Result2 result) {
		// Isn't this nicer?
	}
	@Override
	public void error(Error error) {
		// Error checking in one place!
	}
});

public void userCancel() {
	promise.cancel(); // That's it!
}
```
And if the library does support cancellation of methods, it can just listen for a cancellation on the promise.

Implementing Promises
---------------------
So you have lots of code lying around that uses callbacks? Wrapping it up is super easy.
```java
public void asyncWithCallback(Arg arg, Callback callback) {
	...
}

public Promise<Result<MyResult, Error>> asyncWithPromise(Arg arg) {
	final Deferred<Result<MyResult, Error>> deferred = new Deferred<Result<MyResult, Error>>();
	asyncWithCallback(arg, new Callback() {
		@Override
		public void onResult(MyResult result) {
			deferred.resolve(Result.<MyResult, Error>success(result));
		}
		@Override
		public void onError(Error error) {
			deferred.resolve(Result.<MyResult, Error>error(error));
		}
	});
	return deferred.promise();
}
```

Progress
--------
If you have to return multiple results over time, you can use a `Progress` instead of a `Promise`. Note that a `Progress` can only have one listener.

```java
Progress<Integer> progress = asyncProgress();
progress.listen(new Progress.Listener<Integer>() {
    @Override
    public void receive(Integer message) {
        // This is called on each progress update.
    }
});
```

Like using `Deferred` with `Promise`, you use `Channel` with `Progress`. Unlike `Promise` however, you need to make sure you close your `Channel` when you are done sending messages.
```java
public Progress<Result<MyProgress, Error>> asyncWithPromise(Arg arg) {
    final Channel<Result<MyProgress, Error>> channel = new Channel<Result<MyProgress, Error>>();
	asyncWithProgressCallback(arg, new Callback() {
		@Override
		public void onProgress(MyProgress progress) {
			channel.send(Result.<MyProgress, Error>success(progress));
		}
		@Override
		public void onFinish() {
		    channel.close();
		}
		@Override
		public void onError(Error error) {
			channel.send(Result.<MyProgress, Error>error(error));
		}
	});
	return channel.progress();
}
```

Cancellation
------------
If you have or want to create async methods that support cancellation, you need to use a `CancelToken`. This ensures the cancel propagates to all Promises.
```java
public Promise<Integer> mySuperSlowMethod() {
	final CancelToken cancelToken = new CancelToken();
	final Deferred<Integer> deferred = new Deferred<Integer>(cancelToken);
	new Thread(new Runnable() {
		int total = 0;
		for (int i = 0; i < BAZZILION; i++) {
			if (cancelToken.isCanceled()) break;
			total += i // Do some hard work
		}
		deferred.resolve(total);
	}).start();
	return deferred.promise();
}

public Promise<MyResult> yourSuperSlowMethod() {
	final CancelToken cancelToken = new CancelToken();
	final Deferred<MyResult> deferred = new Deferred<MyResult>(cancelToken);
	final Callback callback = new Callback() {
		@Override
		public void onResult(MyResult result) {
			deferred.resolve(result);
		}
	});
	cancelToken.listen(new CancelToken.Listener() {
		@Override
		public void canceled() {
			methodQueue.cancel(callback);
		}
	});
	methodQueue.add(callback);
	methodQueue.start();
	return deferred.promise();
}
```
A `Progress` can be canceled the same way.

A Note on Memory Usage
----------------------
Both `Promise` and `Progress` keep internal state about their results so that you can attach a listener at any time. This however can make it easy to leak memory.

A `Promise` will store it's result forever. If this result is potentially very large, make sure that you don't keep a reference to the `Proimse` arround after the result is recieved. This could be made tricky by the fact that `Deferred` keeps a reference its `Promise`. For that reason, async code should never hold a refernce to `Defererd` longer than required.

A `Progress` will buffer messages until a listener is attached. This means you must always call either `listen()` or `cancel()` on a `Progress`.

Also remember that anonymous inner classes (which you are probably using for callbacks) will keep a reference to their outer class even if you don't reference anything from it. This means that the outer class will not be garbage collected until the `Promise` completes or the `Channel` for a `Progress` is closed. The easiest way arround this is to call `cancel()` on the `Promise` or `Progress` when it's no longer needed.