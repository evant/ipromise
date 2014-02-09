ipromise
======
Writing asynchronous code can quickly become painful, especially when you need
to make multiple calls, due to each method taking it's own callback and having
it's own idea of error handling and cancellation. Promises alleviate this by
providing a uniform interface and by turning callback style into the same return
style of synchronous code. You can view the javadoc at
http://evant.github.com/ipromise

- [Install](#install)
- [Features](#features)
- [Usage](#usage)
- [Implementing Promises](#implementing-promises)
- [Progress](#progress)
- [Cancellation](#cancellation)
- [Callback Execution](#callback-execution)
- [Android](#android)
- [A Note on Memory Usage](#a-note-on-memory-usage)

Install
-----------
Clone this library, then run
```bash
gradle install
```

### Gradle
```groovy
repositories {
  mavenLocal()
}

dependencies {
  compile 'me.tatarka.ipromise:ipromise:1.0-SNAPSHOT'
}
```

Or for Android
```groovy
dependencies {
  compile 'me.tatarka.ipromise:ipromise-android:1.0-SNAPSHOT'
}
```

### Maven
```xml
<dependency>
  <groupId>me.tatarka.ipromise</groupId>
  <artifactId>ipromise</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Or for Android
```xml
<dependency>
  <groupId>me.tatarka.ipromise</groupId>
  <artifactId>ipromise-android</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Features
--------
This library has a few unique features to give you control over your
asynchronous code.

1. You can send multiple messages to show progress, etc.
2. Error control is out not backed in to the promises. Instead you can
represent errors using a `Result<T,E>`. This way you are free to use promises
in contexts where you know an Exception will never be thrown.
3. You can control how messages are buffered for redelivery when a listener is
attached. The default is just to save the last one, but you can save all of
them, none of them, or implement your own scheme. (see `PromiseBuffer`).
4. You can control in what context callbacks are executed. By default they are
executed on a single background thread; but you can, for example, run them on
your UI thread so you don't have to worry about posting them back.
5. Special consideration has been given to Android to handle the Activity
lifecycle so you don't run into the same pitfalls as you do with `AsyncTask`


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
See how it reads nice and sequentially instead of heavily indented?  If that
doesn't look like a huge improvement to you, lets add some error checking. After
all, `async1()` and `async2()` may fail.

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
See how the error handling is all over the place? And you would have duplicate
code if your error handling in both places was the same.
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
Much nicer! Now lets say the user decides they don't want to wait anymore and
cancels the action.
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
And if the library does support cancellation of methods, it can just listen for
a cancellation on the promise.

Implementing Promises
---------------------
So you have lots of code lying around that uses callbacks? Wrapping it up is
super easy.
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

You can also use a `Task` to easily run code in a separate thread and return a
`Proimse`.
```java
// Runs in a seperate thread 
public Proimse<MyResult> async() {
  return Tasks.run(new Task.DoOnce<MyResult>() {
    @Override
    public MyResult runOnce(CancelToken cancelToken) {
      return sync();
    }
  });
}

// Handles exceptions 
public Promise<Result<MyResult, Error>> asyncError() {
  return Tasks.run(new Task.DoOnceFailable<MyResult, Error>() {
    @Override
    public MyResult runFailable(CancelToken cancelToken) throws Error {
      return syncThatThrows();
    }
  });
}
```

You can also pass an `Executor` to a `Task` for more control.

Progress
--------
If you have to return multiple results over time, just use `send()` instead of
`resolve()`. You do, however need to make sure to call `close()` when you are
finished sending. 

```java
public Promise<MyProgress> asyncWithPromise(Arg arg) {
  final Deferred<MyProgress> deferred = new Deferred<MyProgress>();
  asyncWithProgressCallback(arg, new Callback() {
    @Override
    public void onProgress(MyProgress progress) {
		  deferred.send(progress);
		}
		@Override
		public void onFinish() {
		  deferred.close();
		}
	});
	return channel.progress();
}
```

On the receiving end, you can attach a `CloseListener` to respond to when the
promise has been closed. 

```java
promise.listen(new Listener<MyProgress>() {
  @Override
  public void receive(MyProgress progress) {
    // This is called with every progress update.
  }
}).onClose(new CloseListener() {
  @Override
  public void close() {
    // This is called when there will be no more updates.
  }
});
```

There are various different ways to handle old messages when multiple ones are
sent. For this reason, you can pass a `PromiseBuffer` to a `Deferred` which will
determine how messages are buffered and redelivered when a listener is attached.

For the common cases you can pass in an enumeration.

- `Proimse.BUFFER_NONE` - This means that if there is no listener when a message
  is sent, the message is dropped.
- `Promise.BUFFER_LAST` - This means the last message is saved. This is the
- default since it acts like most promise implementations when one message is
  sent.
- `Progress.REATIN_ALL` - This means all messages are saved.

You can also pass in your own instance of `PromiseBuffer` if you need more
control.

When choosing a buffer strategy keep in mind that all messages saved will be
kept in memory until the `Promise` is garbage collected.

Cancellation
------------
If you have or want to create asynchronous methods that support cancellation,
you need to use a `CancelToken`. This ensures the cancel propagates to all
Promises.
```java
public Promise<Integer> mySuperSlowMethod() {
	final CancelToken cancelToken = new CancelToken();
	final Deferred<Integer> deferred = new Deferred<Integer>(cancelToken);
	new Thread(new Runnable() {
		long total = 0;
		for (long i = 0; i < BAZZILION; i++) {
			if (cancelToken.isCanceled()) break;
			total += i; // Do some hard work
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

Callback Execution
------------------
As mentioned in the feature section, callbacks are not executed in the calling
context, but instead by using an `Executor`. This makes them much easier to
reason about since their execution context is always the same. By default all
callbacks are executed on a single background thread, but you can configure this
is a few different ways.

The easiest way is to call `Promise.setDefaultCallbackExecutor(executor)` which
sets it globally for all promises. This should only be called once for you
application, though this is not enforced. If you need more granular control you
can create a builder with `Deferred.Builder.withCallbackExecutor(executor)` and
then call `build()` to get a deferred with your `Executor`. Finally, `Deferred`
has a constructor that takes an `Executor` as an argument.

When using a custom `Executor`, you must ensure that all jobs are run in the
same order they are posted for a given `Promise`. If you don't you risk the
callbacks being fired in an unexpected order.

For Android, it's a good idea to use the provided
`AndroidPromiseExecutors.mainLooperCallbackExecutor()` which will run all
callbacks on the UI thread.

For unit testing, you can use `Promise.getSameThreadExecutor()` to run callbacks
on the same thread as the messages that are sent. This way you can send a
message and ensure the callback is called without using any thread
synchronization.

Android
-------
Managing the Activity lifecycle in Android with asynchronous calls can be very
tricky. Loaders improve this, but fall short in many situations and have a
clunky api. Instead, you can use the `AsyncManager`. It will handle Activity
destruction, configuration changes, and posting results to the UI thread.

If you just want to load data in the background and show it on screen when you
are done, it is incredibly easy.
```java
public class MyActivity extends Activity {
  private AsyncManager asyncManager;

  public void onCreate(Bundle savedInstanceState) {
    // If you are using the support library, use AsyncManagerCompat.get(this)
    asyncManager = AsyncManager.get(this);
    asyncManager.start(Task.of(mySlowTask), new AsyncAdapter<String>() {
      @Override
      public void start() {
        // This is where you would show your progress indicator. This is called
        // when the promise starts, and on configuration change if the promise
        // hasn't completed.
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
      }

      @Override
      public void receive(String result) {
        // This is where you will get your result. This is called when the
        // promise completes on configuration changes if the promise has
        // completed.
        findViewById(R.id.progress).setVisibility(View.INVISIBLE);
      }
    });
  }

  // It is important that this does not have a reference to surrounding Activity
  // to prevent memory leaks
  private static Task.DoOnce<String> mySlowTask = new Task.DoOnce<String>() {
    @Override
    public String run(CancelToken cancelToken) {
      return doSomeSlowWork();
    }
  };
}
```

Loading/Reloading on a button press is similarly easy. Try doing that with
loaders!
```java
public clas MyActivity extends Activity {
  // You can pass a tag to the PromiseManager to allow multiple tasks/callbacks
  // If you don't pass one, PromiseManager.DEFAULT is used.
  private static final String MY_TASK = "my_task"

  private AsyncManager asyncManager;

  public void onCreate(Bundle savedInstanceState) {
    asyncManager = AsyncManager.get(this);
    final AsyncItem buttonAsync = asyncManager.add(
      MY_TASK, 
      new Task<String>() {
        @Override
        public Promise<String> start() {
          return doAsyncWork();
        }
      },
      new AsyncAdapter<String>() {
        @Override
        public void start() {
          findViewById(R.id.progress).setVisibility(View.VISIBLE);
        }

        @Override
        public void receive(String result) {
          findViewById(R.id.progress).setVisibility(View.INVISIBLE);
        }
      }
    );

    findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        buttonAsync.restart();
      }
    }
  }
}
}
```

You can see more examples on how to use `AsyncManager` in
`ipromse-android-example`

A Note on Memory Usage
----------------------
This library does it's best effort to reduce memory usage by removing listeners 
when they are no longer needed and only keeping one copy of the messages in the
buffer even when you `Map` or `Filter`. However, you need to keep in mind a few
things to make sure you don't leak any memory.

By default, a `Promise` will store it's last result for as long is it lives.
You can configure this by passing a different `PromiseBuffer` to the `Deferred`.
This means that you should not keep a reference `Promise` or `Deferred` longer
than necessary.

You should also remember that anonymous inner classes (which you are probably
using for callbacks) will keep a reference to their outer class even if you
don't reference anything from it. This means that the outer class will not be
garbage collected until the `Promise` is closed or canceled.
