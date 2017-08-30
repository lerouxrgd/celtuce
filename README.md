# celtuce

An idiomatic Clojure Redis client wrapping the Java client [Lettuce][].

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/celtuce.svg)](https://clojars.org/celtuce)

### Redis Connectors

Connectors are available for both Redis `Server` and `Cluster`.

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

Other connectors options:

* `:timeout` timeout for executing commands
* `:unit` corresponding `java.util.TimeUnit`
* `:auto-flush` automatically flush commands on the underlying netty connection

### Redis Commands

All Redis commands are implemented using protocols in `celtuce.commands`.

```clj
(require '[celtuce.commands :as redis])
```

**Sync**

```clj
(def connector (conn/redis-server "redis://localhost:6379"))
(def cmds (conn/commands-sync connector))

(redis/set cmds :foo "bar")
(redis/get cmds :foo)

(conn/shutdown connector)
```

**Async**

Asynchronous execution is wrapped with [Manifold][] `deferred` values that allows for
flexible composition.

```clj
(def connector (conn/redis-server "redis://localhost:6379"))
(def cmds (conn/commands-async connector))

@(redis/set cmds :foo "bar")
@(redis/get cmds :foo)

(conn/shutdown connector)
```

**PubSub**

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

## Tests

To run unit tests you need to have both a redis server running on a `localhost:6379`,
and a redis cluster running on `localhost:30001`.

Then simply run:

```sh
lein test
```

## License

* [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
* Wrapper of https://github.com/lettuce-io/lettuce-core

[lettuce]: https://github.com/lettuce-io/lettuce-core
[wiki-uri]: https://github.com/lettuce-io/lettuce-core/wiki/Redis-URI-and-connection-details#uri-syntax
[nippy]: https://github.com/ptaoussanis/nippy
[manifold]: https://github.com/ztellman/manifold
