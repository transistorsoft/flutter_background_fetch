## 0.2.0 - 2019-03-15
* Use AndroidX.

## 0.1.2 - 2019-02-28
* Fixed bug with setting `jobServiceClass` using a reference to `HeadlessJobService.class`.  This crashes devices < api 21, since Android's `JobService` wasn't available until then.  Simply provide the class name as a `String`.

## 0.1.1 - 2018-11-21
* Fixed issue with Android headless config.

## 0.1.0

* First working implementation

## 0.0.1

* First working implementation
