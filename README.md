# FIXYou [![Build and run integration tests](https://github.com/zlooo/FIXYou/actions/workflows/build-integrationTest.yml/badge.svg)](https://github.com/zlooo/FIXYou/actions/workflows/build-integrationTest.yml) [![codecov](https://codecov.io/gh/zlooo/FIXYou/branch/master/graph/badge.svg)](https://codecov.io/gh/zlooo/FIXYou) [![Known Vulnerabilities](https://snyk.io/test/github/zlooo/FIXYou/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/zlooo/FIXYou?targetFile=build.gradle) [![Total alerts](https://img.shields.io/lgtm/alerts/g/zlooo/FIXYou.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/zlooo/FIXYou/alerts/) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/zlooo/FIXYou.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/zlooo/FIXYou/context:java) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.zlooo.fixyou/netty/badge.svg)](https://mvnrepository.com/artifact/io.github.zlooo.fixyou)


## Overview
FIXYou is yet another fix engine. The purpose of this project is to create an open source fix engine that will be faster than Quickfix, generate less garbage and become the first choice when searching for free, performant fix engine that is suitable for low latency applications.

This library is still in its infancy stage. By no means it's production ready, nor is it feature complete. However, please try it out in non-critical applications, test tools etc and let me know what you think. Contact info is at the end of this file.

## Getting started
You can find FIXYou on [https://mvnrepository.com/](https://mvnrepository.com/artifact/io.github.zlooo.fixyou)

Attach netty module as dependency
* Gradle: 

`implementation group: 'io.github.zlooo.fixyou', name: 'netty', version: '0.1.0'`
* Maven:
```xml
<dependency>
    <groupId>io.github.zlooo.fixyou</groupId>
    <artifactId>netty</artifactId>
    <version>0.1.0</version>
</dependency>
```
Now you can start an `Engine`. Concept is pretty simmilar to `Connector` in Quickfix. Create an `Engine` acceptor instance
```java
final Engine engine = FIXYouNetty.create(FIXYouConfiguration.builder().acceptorBindInterface(bindInterface).acceptorListenPort(port).initiator(false).build(), fixMessageListener);
```
or in case of initiator
```java
final Engine engine = FIXYouNetty.create(FIXYouConfiguration.builder().initiator(true).build(), fixMessageListener);
```
register session and dictionary if needed
```java
engine.registerSessionAndDictionary(sessionID, "fix50sp2", new FixSpec50SP2(), SessionConfig.builder().host(host).port(port).build());//host and port are only needed if you're registering initiator
```
start FIXYou
```java
engine.start();
```
Both sessions and dictionaries can be registered after engine is started.

In order to register session you either need id of an already registered dictionary, `dictionaryID` param of `registerSessionAndDictionary` method, or register a new dictionary. Each FIX dictionary has a unique ID  and instance of FixSpec class. I've written a simple tool https://github.com/zlooo/FIXYou-tools/tree/master/fix_spec_generator that can generate implementation of FixSpec interface based on Quickfix xml dictionary. For your convenience I've also created FixSpec classes for all standard dictionaries. You can find them on [https://mvnrepository.com/](https://mvnrepository.com/artifact/io.github.zlooo.fixyou), just add one as dependency and you're good to go.

`fixMessageListener` that's passed when `Engine` is created is a main entry point to your application. The idea is practically identical to Quickfix's `quickfix.Application`. In FIXYou case each time a new non admin FIX message is received onFixMessage method is invoked.

To send fix message from your application use `FIXYouNetty.sendMessage`, for example
```java
FIXYouNetty.sendMessage(FixMessages.createFIXYouNewOrderSingle(clordid), fixYouSessionId, engine)
```
First parameter is an `Consumer<FixMessage>` that's supposed to set all fields on a message that you want to send.

More details on usage and design can be found on [FIXYou wiki](https://github.com/zlooo/FIXYou/wiki)

## Performance - Yeah right, performance my ass
I have to be honest with you I've done only preliminary performance tests, and they only consist of 2 scenarios, `newOrderSingleSending` and `quoteStreaming` described [here](https://github.com/zlooo/FIXYou-tools#probe-test-scenarios). However, those initial results are pretty encouraging, they say that FIXYou is not slower than Quickfix, and in same cases, can be up to **73% faster**. More details on this subject, including detailed results, can be found on [FIXYou wiki](https://github.com/zlooo/FIXYou/wiki/Performance-Tests).

## Limitations
As mentioned earlier, FIXYou is still work in progress, so it lacks some features you'd normally expect FIX engine to have. The list includes, but is not limited to:
* message persistence - only in memory message store implementation is provided. However, you're able to plug in your own implementations
* session times - currently all sessions are "infinite"
* message encryption
* nested repeating groups - currently FIXYou supports only 1 level of nesting. IE a repeating group can have nested repeating group, but 1 more level, repeating group in a repeating group that's also in releating group, is not supported
* I'm still working on improving documentation. Please let me know if you notice anything missing, and I can assure you that not everything is documented ;)

## Want to help out?
There are various ways you can help in development of FIXYou.
1. Try it our and provide me with some feedback
2. If you liked it, spread the word. Let others know about FIXYou existence
3. Noticed any bug? Please raise an issue on GitHub
4. There are lots of TODOs in the code. Feel free to submit a PR
5. Let me know what features are important to you, I'll try to implement them
6. Just let me know you've used it. Knowing that somebody actually tried FIXYou out is a great motivation to keep going

## Contact info [![Hits](https://hits.seeyoufarm.com/api/count/incr/badge.svg?url=https%3A%2F%2Fgithub.com%2Fzlooo%2FFIXYou&count_bg=%2379C83D&title_bg=%23555555&icon=&icon_color=%23E7E7E7&title=hits&edge_flat=false)](https://hits.seeyoufarm.com)
zlooo.inc[you know what goes here]gmail.com

---
[![YourKitLogo](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com)  
YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).
