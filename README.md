# FIXYou [![Build Status](https://travis-ci.org/zlooo/FIXYou.svg?branch=master)](https://travis-ci.org/zlooo/FIXYou.svg?branch=master) [![codecov](https://codecov.io/gh/zlooo/FIXYou/branch/master/graph/badge.svg)](https://codecov.io/gh/zlooo/FIXYou)

## Overview
FIXYou is yet another fix engine. The purpose of this project is to create an open source fix engine that will be faster than Quickfix, generate less garbage and become the first choice when searching for free, performant fix engine that is suitable for low latency applications.

This library is still in its infancy stage. By no means it's production ready, nor is it feature complete. However, please try it out in non-critical applications, test tools etc and let me know what you think. Contact info is at the end of this file.

## Getting started
You can find FIXYou on https://mvnrepository.com/

Attach netty module as dependency
* Gradle: 

`implementation`
* Maven:
```xml
<dependency></dependency>
```
Now you can start an `Engine`. Concept is pretty simmilar to `Connector` in Quickfix. Create an `Engine` acceptor instance
```java
final Engine engine = FIXYouNetty.create(FIXYouConfiguration.builder().acceptorBindInterface(bindInterface).acceptorListenPort(port).initiator(false).build(), fixMessageListener);
```
or on case of initiator
```java
final Engine engine = FIXYouNetty.create(FIXYouConfiguration.builder().initiator(true).build(), fixMessageListener);
```
register session and dictionary if needed
```java
engine.registerSessionAndDictionary(sessionID, "fix50sp2", new FixSpec50SP2(), new SessionConfig().setHost(host).setPort(port));//host and port are only needed if you're registering initiator
```
start FIXYou
```java
engine.start();
```
Both sessions and dictionaries can be registered after engine is started.

In order to register session you either need id of an already registered dictionary, `dictionaryID` param of `registerSessionAndDictionary` method, or register a new dictionary. Each FIX dictionary has a unique ID  and instance of FixSpec class. I've written a simple tool https://github.com/zlooo/FIXYou-tools/tree/master/fix_spec_generator that can generate implementation of FixSpec interface based on Quickfix xml dictionary.

`fixMessageListener` that's passed when `Engine` is created is a main entry point to your application. The idea is practically identical to Quickfix's `quickfix.Application`. In FIXYou case each time a new FIX message is received onFixMessage method is invoked.

To send fix message from your application use `FIXYouNetty.sendMessage`, for example
```java
FIXYouNetty.sendMessage(FixMessages.createFIXYouNewOrderSingle(clordid), fixYouSessionId, engine)
```
First parameter is an `Consumer<FixMessage>` that's supposed to set all fields on a message that you want to send.

## Performance - Yeah right, performance my ass
I have to be honest with you I've done only preliminary performance tests, and they only consist of 1 scenario, `newOrderSingleSending` described [here](https://github.com/zlooo/FIXYou-tools#probe-test-scenarios). However those initial results are pretty encouraging, they say that FIXYou is not slower than Quickfix, and in same cases, can be up to **50% faster**.

To send fix message from your application use `FIXYouNetty.sendMessage`, for example
```java
FIXYouNetty.sendMessage(FixMessages.createFIXYouNewOrderSingle(clordid), fixYouSessionId, engine)
```
First parameter is an `Consumer<FixMessage>` that's supposed to set all fields on a message that you want to send.

## Performance - Yeah right, performance my ass
I have to be honest with you I've done only preliminary performance tests, and they only consist of 1 scenario, `newOrderSingleSending` described [here](https://github.com/zlooo/FIXYou-tools#probe-test-scenarios). However, those initial results are pretty encouraging, they say that FIXYou is not slower than Quickfix, and in same cases, can be up to **50% faster**. The best thing is I haven't done any profiling or special code optimizations. In other words it may turn out it's even faster :).

## Limitations
As mentioned earlier, FIXYou is still work in progress, so it lacks some features you'd normally expect FIX engine to have. The list includes, but is not limited to:
* message persistence - only in memory message store implementation is provided. However, you're able to plug in your own implementations
* session times - currently all sessions are "infinite"
* message encryption
* I'm still working on improving documentation. Please let me know if you notice anything missing, and I can assure you that not everything is documented ;)

## Want to help out?
There are various ways you can help in development of FIXYou.
1. Try it our and provide me with some feedback
2. If you liked it, spread the word. Let others know about FIXYou existence
3. Noticed any bug? Please raise an issue on GitHub
4. There are lots of TODOs in the code. Feel free to submit a PR
5. Let me know what features are important to you, I'll try to implement them
6. Just let me know you've used it. Knowing that somebody actually tried FIXYou out is a great motivation to keep going

## Contact info [![HitCount](http://hits.dwyl.com/zlooo/FIXYou.svg)](http://hits.dwyl.com/zlooo/FIXYou)
zlooo.inc[you know what goes here]gmail.com