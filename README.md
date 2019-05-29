# celtuce

An idiomatic Clojure Redis client wrapping the Java client [Lettuce][].

## Usage

 [![Clojars Project](https://img.shields.io/clojars/v/celtuce.svg)](https://clojars.org/celtuce) to include all [modules][].

Or pick up only the ones you need:

* [celtuce-core][]: Main module with all the core functionalities (required)

  [![Clojars Project](https://img.shields.io/clojars/v/celtuce-core.svg)](https://clojars.org/celtuce-core)
  [![cljdoc badge](https://cljdoc.org/badge/celtuce-core/celtuce-core)](https://cljdoc.org/d/celtuce-core/celtuce-core/CURRENT)

* [celtuce-pool][]: Provides pooling for connections

  [![Clojars Project](https://img.shields.io/clojars/v/celtuce-pool.svg)](https://clojars.org/celtuce-pool)
  [![cljdoc badge](https://cljdoc.org/badge/celtuce-pool/celtuce-pool)](https://cljdoc.org/d/celtuce-pool/celtuce-pool/CURRENT)

* [celtuce-manifold][]: Implementation of asynchronous commands based on [Manifold][]

  [![Clojars Project](https://img.shields.io/clojars/v/celtuce-manifold.svg)](https://clojars.org/celtuce-manifold)
  [![cljdoc badge](https://cljdoc.org/badge/celtuce-manifold/celtuce-manifold)](https://cljdoc.org/d/celtuce-manifold/celtuce-manifold/CURRENT)

### Redis Connectors

Connectors are available for both Redis `Server` and `Cluster`.
They are defined in `celtuce.connector` namespace of `celtuce-core` module.

```clj
(require '[celtuce.connector :as conn])

(conn/redis-server "redis://localhost:6379")

(conn/redis-cluster "redis://localhost:30001")
```

Redis URI synthax details can be found in [Lettuce Wiki][wiki-uri].

Serialization defaults to [Nippy][], but other serializers are available in `celtuce.codec`.
Especially [Lettuce][] original `String` serializer can be used as follows:

```clj
(conn/redis-server
  "redis://localhost:6379"
  :codec (celtuce.codec/utf8-string-codec))
```

Other connector options:

* `:conn-options` a map of connection options
  * `:timeout` timeout for executing commands
  * `:unit` corresponding `TimeUnit` in keyword (i.e. `:milliseconds`, etc)
  * `:auto-flush` automatically flush commands on the underlying Netty connection

* `:client-options`: a map of client options
  * [Client-options][] available in Lettuce, with their names keywordized

Note that you can find options default values in the [tests][tests-connector].

### Redis Commands

All Redis [commands][] are implemented using protocols in `celtuce.commands` namespace of `celtuce-core` module.

```clj
(require '[celtuce.commands :as redis])
```

**Sync Commands**

```clj
(def connector (conn/redis-server "redis://localhost:6379"))
(def cmds (conn/commands-sync connector))

(redis/set cmds :foo "bar")
(redis/get cmds :foo)

(conn/shutdown connector)
```

**PubSub Commands**

Redis prevents publishing and subscribing on the same connection.
The following contrive example demonstrates pubsub usage with two connections.

```clj
;; note that conn/as-pubsub also works on cluster connectors
(def conn-pub (conn/as-pubsub (conn/redis-server "redis://localhost:6379")))
(def conn-sub (conn/as-pubsub (conn/redis-server "redis://localhost:6379")))

(def pub (conn/commands-sync conn-pub))
(def sub (conn/commands-sync conn-sub))

(conn/add-listener! 
 conn-sub
 (reify redis/PubSubListener
   (message [_ channel message]
     (println "received message" message "from channel" channel))
   (message [_ pattern channel message])
   (subscribed [_ channel count]
     (println "new subscriber !"))
   (unsubscribed [_ channel count]
     (println "a subscriber left..."))
   (psubscribed [_ pattern count])
   (punsubscribed [_ pattern count])))

(redis/subscribe sub "foo-chan")
(redis/publish pub "foo-chan" "bar-msg")
(redis/unsubscribe sub "foo-chan")

(conn/shutdown conn-pub)
(conn/shutdown conn-sub)
```

**Dynamic Commands**

Starting from Lettuce 5 it is now possible to define commands [dynamically][dynamic] by extending a `Commands` interface.
Such commands are obtained as follows.

```clj
(conn/commands-dynamic connector some.interface.extending.Commands)
```

You can find basic examples in the tests.

## Tests

To run unit tests you need to have both a redis server running on a `localhost:6379`,
and a redis cluster running on `localhost:30001`.

Then build artifacts and run tests:

```sh
(cd modules/celtuce-core/; lein do clean, install)
(cd modules/celtuce-pool/; lein do clean, install)
(cd modules/celtuce-manifold/; lein do clean, install)

lein test
```

## License

* [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[lettuce]: https://github.com/lettuce-io/lettuce-core
[wiki-uri]: https://github.com/lettuce-io/lettuce-core/wiki/Redis-URI-and-connection-details#uri-syntax
[client-options]: https://github.com/lettuce-io/lettuce-core/wiki/Client-options
[dynamic]: https://github.com/lettuce-io/lettuce-core/wiki/Redis-Command-Interfaces

[modules]: https://github.com/lerouxrgd/celtuce/tree/master/modules
[commands]: https://github.com/lerouxrgd/celtuce/blob/master/modules/celtuce-core/src/celtuce/commands.clj
[celtuce-core]: https://github.com/lerouxrgd/celtuce/tree/master/modules/celtuce-core
[celtuce-pool]: https://github.com/lerouxrgd/celtuce/tree/master/modules/celtuce-pool
[celtuce-manifold]: https://github.com/lerouxrgd/celtuce/tree/master/modules/celtuce-manifold
[tests-connector]: https://github.com/lerouxrgd/celtuce/blob/master/test/celtuce/connector_test.clj

[nippy]: https://github.com/ptaoussanis/nippy
[manifold]: https://github.com/ztellman/manifold
