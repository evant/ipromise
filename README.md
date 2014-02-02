ipromise
======
Writing asynchronous code can quickly become painful, especially when you need
to make multiple calls, due to each method taking it's own callback and having
it's own idea of error handling and cancellation. Promises alleviate this by
providing a uniform interface and by turning callback style into the same return
style of synchronous code. You can view the javadoc at
http://evant.github.com/ipromise

- [Install](#install)
- [Usage](#usage)
- [Implementing Promises](#implementing-promises)
- [Progress](#progress)
- [Cancellation](#cancellation)
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

You can also use a `Task` to easily run code in a seperate thread and return a
`Proimse`.
```java
// Runs in a seperate thread 
public Proimse<MyResult> async() {
  return Tasks.run(new Do<MyResult>() {
    @Override
    public MyResult run(CancelToken cancelToken) {
      return sync();
    }
  });
}

// Handles exceptions 
public Promise<Result<MyResult, Error>> asyncError() {
  return Tasks.run(new DoFailable<MyResult, Error>() {
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
If you have to return multiple results over time, you can use a `Progress`
instead of a `Promise`.

```java
Progress<Integer> progress = asyncProgress();
progress.listen(new Progress.Listener<Integer>() {
    @Override
    public void receive(Integer message) {
        // This is called on each progress update.
    }
});
```

Like using `Deferred` with `Promise`, you use `Channel` with `Progress`. Unlike
`Promise` however, you need to make sure you close your `Channel` when you are
done sending messages.
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

There are various different ways to handle old messages on a `Progress` for
this reason, the `Channel` constructor can take a retention policy. There are
3 options.

- `Progress.RETAIN_NONE` - This means that if there is no listener when a
  a message is sent, the message is dropped.
- `Progress.REATIN_LAST` - This means the last message is saved. This message
  will immediatly be delivered to the listener when it is attached.
- `Progress.REATIN_ALL` - This means all messages are saved and will be deliverd
  to the listener when it is attached.

When choosing a retention policy keep in mind that all messages saved will be
kept in memory until the `Progress` is garbage collected.

Cancellation
------------
If you have or want to create async methods that support cancellation, you need
to use a `CancelToken`. This ensures the cancel propagates to all Promises.
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
A `Progress` can be canceled the same way.

Android
-------
Managing the Activity lifecycle in Android with asynchronous calls can be very
tricky. Loaders improve this, but fall short in many situations and have a
cluncky api. Instead, you can use the `AsyncManager`. It will handle Activity
destruction, configuration changes, and posting results to the UI thread.

If you just want to load data in the backround and show it on screen when you
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
  private static Task.Do<String> mySlowTask = new Task.Do<String>() {
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
Both `Promise` and `Progress` keep internal state about their results so that
you can attach a listener at any time. This however can make it easy to leak
memory.

A `Promise` will store it's result forever. If this result is potentially very
large, make sure that you don't keep a reference to the `Proimse` arround after
the result is recieved. This could be made tricky by the fact that `Deferred`
keeps a reference its `Promise`. For that reason, async code should never hold a
refernce to `Defererd` longer than required.

A `Progress` will store messages depending on it's set retention policy. Keep in
mind which one you choose to control your memory usage.

Also remember that anonymous inner classes (which you are probably using for
callbacks) will keep a reference to their outer class even if you don't
reference anything from it. This means that the outer class will not be garbage
collected until the `Promise` completes or the `Channel` for a `Progress` is
closed. The easiest way arround this is to call `cancel()` on the `Promise` or
`Progress` when it's no longer needed.
