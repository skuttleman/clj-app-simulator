# App Simulator

A programmable API simulator for use in manual/automated integration and acceptance testing.

## API

With the exception of the [Resource API](#resource-api), all endpoints support `application/json`, `application/edn`,
and `application/transit` as transport layers for any content sent and received. The `Resource API` uses
`multi-part/form-data` for uploading files and can produce any of the available transport encodings as a response. Set
your desired transport layer via the `Content-Type` and `Accept` HTTP headers. The following examples are all with JSON
encoding (where applicable).

### Resource API

This API is used to add, replace, and delete resources (files) to be used with the
[File Simulator API](#file-simulator-api).

#### `POST /api/resources`

Upload resources (files). Files should sent with the name `files` (even if there is only one) as `multi-part/form-data`.
This produces the `resources/add` event for each file uploaded.

```bash
$ curl -F 'files=@/path/to/file.pdf;filename=example.pdf;type=application/pdf' \
       -F 'files=@/path/to/image.jpg;filename=image.jpg;type=image/jpeg' \
       http://localhost:3000/api/resource
```

##### Response Spec

- `resources`::[Resource](#resource)[]

#### `GET /api/resources`

Get a list of all available file resources.

```bash
$ curl http://localhost:3000/api/resources
```

##### Response Spec

- `resources`::[Resource](#resource)[]

#### `PUT /api/resources/:resource-id`

Place a resource at a specified ID (Must be a canonical UUID string) replacing an existing file if there was one. The
file should be sent with the name `file`. This produces the `resources/put` event.

```bash
$ curl -X 'PUT' \
       -F 'file=@/path/to/example.png;filename=example.png;type=images/png' \
       http://localhost:3000/api/resources/01234567-89ab-cdef-0123-456789abcdef
```

##### Response Spec

- `resource`::[Resource](#resource)

#### `DELETE /api/resources`

Delete all uploaded resources. This produces the `resources/clear` event.

```bash
$ curl -X 'DELETE' http://localhost:3000/api/resources
```

#### `DELETE /api/resources/:resource-id`

Delete a specific resource by ID (as returned from POST and/or GET request). This produces the `resources/remove` event.

```bash
$ curl -X 'DELETE' http://localhost:3000/api/resources/01234567-89ab-cdef-0123-456789abcdef
```

### Simulators API

Create and manage endpoints to be used for various simulators. All simulators are rooted at `/simulators`.

#### `POST /api/simulators/init`

Clear out any existing simulators and initialize zero or more simulator endpoints. Produces the `simulators/init` event.

```bash
$ curl -X 'POST' \
       -H 'Content-Type: application/json' \
       -H 'Accept: application/json' \
       --data '{"simulators":[{"method":"http/put","path":"/resource/:id","response":{"status":204}}]}' \
       http://localhost:3000/api/simulators/init
``` 

##### Request Spec

- `simulators`::[SimulatorConfig](#simulatorconfig)[]

##### Response Spec

- `simulators`::[SimulatorDetails](#simulatordetails)[]

#### `POST /api/simulators`

Add a simulator without destroying previously created simulators. The request will fail if there is an existing
simulator with the same `METHOD` and `PATH`. This produces the `simulators/add` event.

```bash
$ curl -X 'POST' \
       -H 'Content-Type: application/json' \
       --data '{"simulator":{"method":"http/post","path":"/resource","response":{"status":201,"body":"{\"some\":\"json\"}","headers":{"x-custom-header":"header-value"}},"delay":3}}' \
       http://localhost:3000/api/simulators
```

##### Request Spec

- `simulator`::[SimulatorConfig](#simulatorconfig)

##### Response Spec

- `simulator`::[SimulatorDetails](#simulatordetails)


#### `GET /api/simulators`

Get simulators with all stored activity.

```bash
$ curl http://localhost:3000/api/simulators
```

##### Response Spec

- `simulators`::[SimulatorDetails](#simulatordetails)[]

#### `DELETE /api/simulators/reset`

Resets all simulators back to their initial states.

```bash
$ curl -X 'DELETE' http://localhost:3000/api/simulators/reset
```

### HTTP Simulator API

For each HTTP Simulator created, the following API is mounted to allow for interacting with that simulator. Notice that
all simulators are mounted at `/simulators/{simulator-path}`. The `METHOD` denoted below is the HTTP method as that is
the simulator's `method` with the `domain` and `sub-domain` removed i.e. `http/post` is a `POST` request and `file/get`
is a `GET` request.

##### `{METHOD} /simulators/{simulator-path}`

An endpoint for collecting requests and producing the configured response. Requests to this endpoint produce the
`simulators/receive` event.

```bash
$ curl -X '{METHOD}' \
       -H 'Some: header' \
       --data '{"some":"body"}' \
       http://localhost:3000/simulators/some/path?some=query
```

##### `GET /api/simulators/{simulator-id}`

Gets the current configuration and metadata associated with the simulator.

```bash
$ curl http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6
```

###### Response Spec

- `simulator`::[HTTPSimulatorDetails](#httpsimulatordetails)

##### `DELETE /api/simulators/{simulator-id}`

Deletes the simulator. This produces the `simulators/delete` event.

```bash
$ curl -X 'DELETE' http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6
```

##### `PATCH /api/simulators/{simulator-id}`

Updates the simulator. Do not attempt to update a simulator's `path` or `method`. This produces the event that
corresponds to the supplied action. See the [request spec](#httpsimulatorpatch) for more details.

```bash
$ curl -X 'PATCH' \
       --data '{"update":{"action":"simulators/change","config":{"delay":13}}}' \
       http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6
```

###### Request Spec

- `update`::[HTTPSimulatorPatch](#httpsimulatorpatch)

###### Response Spec

- `simulator`::[HTTPSimulatorDetails](#httpsimulatordetails)

### File Simulator API

##### `{METHOD} /simulators/{simulator-path}`

An endpoint for collecting requests and producing the configured resource. Requests to this endpoint produce the
`simulators/receive` event.

```bash
$ curl -X '{METHOD}' \
       -H 'Some: header' \
       http://localhost:3000/simulators/some/file > /tmp/output-file
```

##### `GET /api/simulators/{simulator-id}`

Gets the current configuration and metadata associated with the simulator.

```bash
$ curl http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6
```

###### Response Spec

- `simulator`::[HTTPSimulatorDetails](#httpsimulatordetails)

##### `DELETE /api/simulators/{simulator-id}`

Deletes the simulator. This produces the `simulators/delete` event.

```bash
$ curl -X 'DELETE' http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6
```

##### `PATCH /api/simulators/{simulator-id}`

Updates the simulator. Do not attempt to update a simulator's `path` or `method`. This produces the event that
corresponds to the supplied action. See the [request spec](#filesimulatorpatch) for more details.

```bash
$ curl -X 'PATCH' \
       --data '{"update":{"action":"simulators/change","config":{"delay":13}}}' \
       http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6
```

###### Request Spec

- `update`::[FileSimulatorPatch](#filesimulatorpatch)

###### Response Spec

- `simulator`::[FileSimulatorDetails](#filesimulatordetails)

### WS Simulator API

##### `WS /simulators/{simulator-path}`

An endpoint for connecting web sockets to the simulator. Connecting to and disconnecting from this endpoint produce the
`simulators.ws/connect` and `simulators.ws/disconnect` events respectively.

```bash
$ wscat -c ws://localhost:3000/simulators/some/path
```

##### `GET /simulators/{simulator-id}`

Gets the current configuration and metadata associated with the simulator.

```bash
$ curl http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6
```

###### Response Spec

- `simulator`::[WSSimulatorDetails](#wssimulatordetails)

##### `DELETE /simulators/{simulator-id}`

Deletes the simulator. This produces the `simulators/delete` event.

```bash
$ curl -X 'DELETE' http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6
```

##### `PATCH /simulators/{simulator-id}`

Updates the simulator. Do not attempt to update a simulator's `path` or `method`. This produces the event that
corresponds to the supplied action. See the [request spec](#wssimulatorpatch) for more details.

```bash
$ curl -X 'PATCH' \
       --data '{"update":{"action":"simulators.ws/disconnect"}}' \
       http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6
```

###### Request Spec

- `update`::[WSSimulatorPatch](#wssimulatorpatch)

###### Response Spec

- `simulator`::[WSSimulatorDetails](#wssimulatordetails)

##### `POST /simulators/{simulator-id}`

An endpoint for broadcasting a message to all active web socket clients. The body of the HTTP request is sent as the
body of the socket message.

```bash
$ curl -X 'POST' \
       --data 'Hello to all connected web sockets!' \
       http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6
```

##### `POST /simulators/{simulator-id}/sockets/{socket-id}`

An endpoint for broadcasting a message to a specific web socket client. The body of the HTTP request is sent as the
body of the socket message.

```bash
$ curl -X 'POST' \
       --data 'Hello to one specific web socket!' \
       http://localhost:3000/api/simulators/cca02cae-ae90-4c3a-bc63-c36da5b9efa6/sockets/b7de4aa4-73b0-487c-82bf-ca96187380d9
```

## Activity Feed

A web socket activity is available for all activity that modifies the state of the running simulators which includes
CRUD operations on and interactions with the simulators. Each message sent on the activity feed contains an `event` and
optional `data`. Subscribe to the feed via `WS /api/simulators/activity`.

### Events

Events are sent as key balue pairs of `event` and `data`. The `event` identifier will contain a `domain`, an optional
`sub-domain`, and a `name` in the format `domain[.sub-domain]/name`. Each distinct `event` will have a corresponding
schema for its `data` model as outlined below.

```json
{
  "event": "resources/add",
  "data": {
    "resource": {
      "id": "a36c8a06-6f4f-4b74-9e3d-6baa35089242",
      "filename": "image.gif",
      "timestamp": "1970-01-01T00:00:00.000Z",
      "content-type": "image/gif"
    }
  }
}
```

#### `resources/add`, `resources/put`, and `resources/remove`

- `resource`::[Resource](#resource)

#### `resources/clear`

There is no data for this event.

#### `simulators/add`, `simulators/change`, `simulators/delete`, `simulators/reset`, and `simulators/receive`

- `simulator`::[SimulatorDetails](#simulatordetails)

#### `simulators/init` and `simulators/reset-all`

- `simulators`::[SimulatorDetails](#simulatordetails)[]

#### `simulators.ws/connect`

- `simulator`::[WSSimulatorDetails](#wssimulatordetails)

#### `simulators.ws/disconnect`

- `simulator`::[WSSimulatorDetails](#wssimulatordetails)
- `socket-id`::[UUID](#uuid) _(optional)_ - the ID of the web socket that has been disconnected. No `socket-id` means
that all web sockets were disconnected.

## Type Specifications

A type specification for all data accepted and returned by the API. A note for `JSON` users: `Inst`s, `UUID`s and
`Enum`s are transmitted as `String`s. For `EDN` and `Transit` users they are transmitted as `UUID`s, `Insts` and
`Keyword`s respectively.

### URI

The path of the simulator expressed in a String. Parameterized portions of the URI are prefixed with a colon `:`. 

`/`, `/some/path`, `/some/path/with/:param`, and `/some/:param1/with/:param2`

### UUID

A canonical representation of a UUID.

`1d4e0e8f-44b2-49a2-d1d9-6dd9660cbadd`

### Inst

A date/time instant represented in ISO8601 format.

`1970-01-01T00:00:00.000Z` or `1970-01-01T00:00:00.000-00:00`

### Resource

- `id`::[UUID](#uuid) - the unique identifier of the resource
- `filename`::String - the filename of the resource
- `timestamp`::[Inst](#inst) - the date and time the resource was uploaded
- `content-type`::String - the mime-type to be used as the content-type by any file simulator that serves this resource
as the body of its response

```json
{
  "id": "a36c8a06-6f4f-4b74-9e3d-6baa35089242",
  "filename": "image.gif",
  "timestamp": "1970-01-01T00:00:00.000Z",
  "content-type": "image/gif"
}
```

### SimulatorConfig

An abstract representation of a simulator's configuration. Can be one of [HTTPSimulatorConfig](#httpsimulatorconfig),
[FileSimulatorConfig](#filesimulatorconfig), or [WSSimulatorConfig](#wssimulatorconfig). Any additional keys and values
supplied when creating or updating a simulator will be stored with the simulator as metadata and returned as part of its
config and details responses.

### HTTPSimulatorConfig

- `method`::Enum{ `http/get` | `http/post` | `http/patch` | `http/put` | `http/delete` } - HTTP method used to
communicate with the simulator
- `path`::[URI](#uri) - endpoint where the simulator will be mounted. The value supplied will be prefixed by the server
with `/simulators`
- `delay`::Integer{ 0 <= `delay` } _(optional)_ - number of milliseconds to wait before producing a response. Defaults
to `0`
- `response`::[HTTPResponse](#httpresponse) - configures the response of the simulator

```json
{
  "method": "http/post",
  "path": "/some/:path",
  "delay": 13,
  "response": {
    "status": 201
  }
}
```

### FileSimulatorConfig

- `method`::Enum{ `file/get` | `file/post` | `file/patch` | `file/put` | `file/delete` } - HTTP method used to
communicate with the simulator
- `path`::[URI](#uri) - endpoint where the simulator will be mounted. The value supplied will be prefixed by the server
with `/simulators`
- `delay`::Integer{ 0 <= `delay` } _(optional)_ - number of milliseconds to wait before producing a response. Defaults
to `0`
- `response`::[FileResponse](#fileresponse) - configures the response of the simulator

```json
{
  "method": "file/get",
  "path": "/some/:path",
  "response": {
    "status": 200,
    "file": "0b7babbc-6780-4d9e-a781-011f63d14b75"
  }
}
```

### WSSimulatorConfig

- `method`::Enum{ `ws/ws` } - indicates a web socket simulator
- `path`::[URI](#uri) - endpoint where the simulator will be mounted. The value supplied will be prefixed by the server
with `/simulators`

```json
{
  "method": "ws/ws",
  "path": "/some/:path"
}
```

### HTTPResponse

- `status`::Integer { 200 <= `status` < 600 } - the HTTP status code used in the response. This is optional for
simulator updates
- `body`::String _(optional)_ the body of the HTTP response
- `headers`::&lt;String -> {String | String[]}&gt; _(optional)_ - HTTP headers to be included with the response

```json
{
  "status": 200,
  "body": "{\"some\":\"body\"}",
  "headers": {
    "content-type": "application/json",
    "x-custom-header": ["value 1", "value 2"]
  }
}
```

### FileResponse

- `status`::Integer { 200 <= `status` < 600 } - the HTTP status code used in the response. This is optional for
simulator updates
- `file`::[UUID](#uuid) - the ID of the resource to be used as the response body. This is optional for simulator
updates. If the ID is not associated with a resource or the resource has been deleted, an empty body will be produced by
the simulator
- `headers`::&lt;String -> {String | String[]}&gt; _(optional)_ - HTTP headers to be included with the response

```json
{
  "status": 200,
  "file": "2e761e32-c508-445e-b339-728a866c44a5",
  "headers": {
    "date": "Fri, Feb 22 2222 22:22:22 GMT",
    "x-custom-header": ["value 1", "value 2"]
  }
}
```

### SimulatorDetails

An abstract representation of a simulator's details. Can be one of [HTTPSimulatorDetails](#httpsimulatordetails),
[FileSimulatorDetails](#filesimulatordetails), or [WSSimulatorDetails](#wssimulatordetails).

### HTTPSimulatorDetails

- `id`::[UUID](#uuid) - the unique identifier of the simulator
- `config`::[HTTPSimulatorConfig](#httpsimulatorconfig) - the simulator's current configuration
- `requests`::[StoredRequest](#storedrequest)[] - all stored requests received by the simulator

```json
{
  "method": "http/post",
  "path": "/some/:path",
  "delay": 13,
  "response": {
    "status": 201
  }
}
```

### FileSimulatorDetails

- `id`::[UUID](#uuid) - the unique identifier of the simulator
- `config`::[FileSimulatorConfig](#filesimulatorconfig) - the simulator's current configuration
- `requests`::[StoredRequest](#storedrequest)[] - all stored requests received by the simulator

### WSSimulatorDetails

- `id`::[UUID](#uuid) - the unique identifier of the simulator
- `config`::[WSSimulatorConfig](#wssimulatorconfig) - the simulator's current configuration
- `requests`::[StoredRequest](#storedrequest)[] - all stored requests received by the simulator
- `sockets`::[UUID](#uuid)[] - unique IDs of all connected web sockets

### HTTPSimulatorPatch

An abstract representation of an HTTP simulator's update options. Can be one of
[HTTPSimulatorChangePatch](#httpsimulatorchangepatch) or [HTTPSimulatorResetPatch](#httpsimulatorresetpatch). Any
additional keys and values supplied when creating or updating a simulator will be stored with the simulator and returned
as part of its config response.
 
### HTTPSimulatorChangePatch

- `action`::Enum{ `simulators/change` } - indicates a change request
- `config`::[HTTPSimulatorChangeConfig](#httpsimulatorchangeconfig) - configuration values to be merged in to the
simulator. Any keys not supplied will retain their current value

```json
{
  "action": "simulators/change",
  "config": {
    "response": {
      "headers": {
        "x-new-header": "new-header-value"
      }
    }
  }
}
```

### HTTPSimulatorResetPatch

- `action`::Enum{ `simulators/reset` } - indicates a reset request
- `type`::Enum{ `http/config` | `http/requests` | `http/response` } _(optional)_ - type of reset to perform. If not
supplied, resets the simulator back to its initial state

```json
{
  "action": "simulators/reset",
  "type": "http/requests"
}
```

### FileSimulatorPatch

An abstract representation of a file simulator's update options. Can be one of
[FileSimulatorChangePatch](#filesimulatorchangepatch) or [FileSimulatorResetPatch](#filesimulatorresetpatch). Any
additional keys and values supplied when creating or updating a simulator will be stored with the simulator and returned
as part of its config response.

### FileSimulatorChangePatch

- `action`::Enum{ `simulators/change` } - indicates a change request
- `config`::[FileSimulatorChangeConfig](#filesimulatorchangeconfig) - configuration values to be changed in the
simulator. Any keys not supplied will retain their current value

```json
{
  "action": "simulators/change",
  "config": {
    "response": {
      "file": "0a22698f-fc83-49e6-a73d-f0baf09fcc7a"
    }
  }
}
```

### FileSimulatorResetPatch

- `action`::Enum{ `simulators/reset` } - indicates a reset request
- `type`::Enum{ `file/config` | `file/requests` | `file/response` } _(optional)_ - type of reset to perform. If not
supplied, resets the simulator back to its initial state

```json
{
  "action": "simulators/reset",
  "type": null
}
``` 

### WSSimulatorPatch
An abstract representation of a web socket simulator's update options. Can be one of
[WSSimulatorChangePatch](#wssimulatorchangepatch), [WSSimulatorResetPatch](#wssimulatorresetpatch), or
[WSSimulatorDisconnectPatch](#wssimulatordisconnectpatch). Any additional keys and values supplied when creating or
updating a simulator will be stored with the simulator and returned as part of its config response.

### WSSimulatorChangePatch

- `action`::Enum{ `simulators/change` } - indicates a change request
- `config`::&lt;String -> Any&gt; - any metadata you wish to update on the simulator. Any keys not supplied will retain
their current value

```json
{
  "action": "simulators/change",
  "config": {
    "name": "A name for this simulator"
  }
}
``` 

### WSSimulatorResetPatch

- `action`::Enum{ `simulators/reset` } - indicates a reset request
- `type`::Enum{ `ws/config` | `ws/requests` } _(optional)_ - type of reset to perform. If not supplied, resets the
simulator back to its initial state and disconnects all active web socket connections

```json
{
  "action": "simulators/reset"
}
``` 

### WSSimulatorDisconnectPatch

- `action`::Enum{ `simulators.ws/disconnect` } - indicates a disconnect request
- `socket-id`::[UUID](#uuid) _(optional)_ - the ID of the web socket to be disconnected. If not supplied, disconnects
all active web sockets

```json
{
  "action": "simulators.ws/disconnect",
  "socket-id": "86154ffd-d686-4262-80d0-9ee49ac282c2"
}
``` 

### StoredRequest

- `id`::[UUID](#uuid) - the unique identifier of the request
- `timestamp`::[Inst](#inst) - the date and time a request was received
- `query-params`::&lt;String -> String&gt; - query params from the request. For WS simulators, these are from the
initial handshake
- `route-params`::&lt;String -> String&gt; - route params from the request. For WS simulators, these are from the
initial handshake
- `headers`::&lt;String -> { String | String[] }&gt; - headers from the request. For WS simulators, these are from the
initial handshake
- `body`::String _(optional)_ - the body of the request or web socket message

```json
{
  "id": "adabbe95-c828-466b-82f5-ed7f32b6f8d0",
  "timestamp": "2222-02-22T22:22:22.222Z",
  "query-params": {
    "search": "term"
  },
  "route-params": {
    "param1": "value-1",
    "param2": "value-2"
  },
  "headers": {
    "content-type": "text/plain"
  },
  "body": "This is some text"
}
```

### HTTPSimulatorChangeConfig

Any additional keys sent with the config will be stored as metadata with the simulator and returned in the simulator's
details response. Do not try to update a simulator's `method` or `path`.

- `delay`::Integer{ 0 <= `delay` } _(optional)_ - the number of milliseconds to wait before producing a response
- `response`::[HTTPResponse](#httpresponse) _(optional)_ - a new response to be merged in with the current configuration

```json
{
  "delay": 200,
  "response": {
    "body": "new body"
  }
}
```

### FileSimulatorChangeConfig

Any additional keys sent with the config will be stored as metadata with the simulator and returned in the simulator's
details response. Do not try to update a simulator's `method` or `path`.

- `delay`::Integer{ 0 <= `delay` } _(optional)_ - the number of milliseconds to wait before producing a response
- `response`::[FileResponse](#fileresponse) _(optional)_ - a new response to be merged in with the current configuration

```json
{
  "delay": 30000,
  "response": {
    "status": 201
  }
}
```
