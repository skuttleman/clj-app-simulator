# App Simulator

A programmable API simulator for use in integration testing.

## Usage

### Download It

### Run It

```bash
$ java -jar clj-app-simulator-standalone.jar
```

Optionally specify the port.

```bash
$ PORT=1234 java -jar clj-app-simulator-standalone.jar
```

Visit the UI in a browser to create, edit, or delete simulators.


## API

The API supports `application/json`, `application/edn`, and `application/transit` as transport layers. The following
examples are all with JSON encoding.

### Resources

Resources are binary files that can be uploaded and used with [file simulators](#file-simulators).

#### `POST /api/resources`

Upload a new resource (file) to be used. Files should sent as `files` (whether there is one or more than one)
as `multi-part/form-data`.

```bash
$ curl -F 'files=@/path/to/file.pdf;filename=example.pdf;type=application/pdf' \
       -F 'files=@/path/to/image.jpg;filename=image.jpg;type=image/jpeg' \
       http://localhost:3000/api/resource
```

This sends a message via `ws://localhost:3000/api/simulators/activity` to all subscribers. A separate message is sent for each file uploaded.

```json
{
  "event": "files.upload/receive",
  "data": {
    "id": "bb90194b-f202-486f-9ca5-8c88385d04d7",
    "filename": "example.pdf",
    "content-type": "application/pdf",
    "timestamp": "1970-01-01T00:00:00Z"
  }
}
```

#### `GET /api/resources`

Get all available file resources.

```bash
$ curl http://localhost:3000/api/resources
#=> {
#=>   "uploads": [{
#=>     "id": "01234567-89ab-cdef-0123-456789abcdef",
#=>     "filename": "file.pdf",
#=>     "content-type": "application/pdf",
#=>     "timestamp": "1970-01-01T00:00:00Z"
#=>   }, {
#=>     "id": "fedcba98-7654-3210-fedc-ba9876543210",
#=>     "filename": "image.jpg",
#=>     "content-type": "image/jpeg",
#=>     "timestamp": "1970-01-01T00:00:00Z"
#=>   }]
#=> }
```

#### `DELETE /api/resources`

Delete all uploaded resources.

```bash
$ curl -X 'DELETE' http://localhost:3000/api/resources
```

This sends a message via `ws://localhost:3000/api/simulators/activity` to all subscribers.

```json
{
  "event": "files/clear",
  "data": null
}
```

#### `DELETE /api/resources/:resource-id`

Delete a specific resource by id (as returned from POST and/or GET request).

```bash
$ curl -X 'DELETE' http://localhost:3000/api/resources/01234567-89ab-cdef-0123-456789abcdef
```

This sends a message via `ws://localhost:3000/api/simulators/activity` to all subscribers.

```json
{
  "event": "files/remove",
  "data": {
    "id": "01234567-89ab-cdef-0123-456789abcdef",
    "filename": "example.pdf",
    "content-type": "application/pdf",
    "timestamp": "1970-01-01T00:00:00Z"
  }
}
```

#### `PUT /api/resources/:resource-id`

Places a resource at a specified id (Must be a canonical UUID string) replacing an existing file if there was one.

```bash
$ curl -X 'PUT' \
       -F 'file=@/path/to/example.png;filename=example.png;type=images/png' \
       http://localhost:3000/api/resources/01234567-89ab-cdef-0123-456789abcdef
```

This sends a message via `ws://localhost:3000/api/simulators/activity` to all subscribers.

```json
{
  "event": "files.upload/replace",
  "data":{
    "id": "01234567-89ab-cdef-0123-456789abcdef",
    "filename": "functionaljavascript.pdf",
    "content-type": "application/pdf",
    "timestamp": "2018-08-24T23:46:37Z"
  }
}
```

### Simulators

Create and manage endpoints to be used for various simulators.

#### `POST /api/simulators/init`

Clear out any existing simulators and initialize zero or more simulator endpoints. See each simulator section for the
different specs. 

```bash
$ curl -X 'POST' \
       -H 'Content-Type: application/json' \
       --data '{"simulators":[{"method":"http/put","path":"/resource/:id","response":{"status":204}}]}' \
       http://localhost:3000/api/simulators/init
```

This sends a message via `ws://localhost:3000/api/simulators/activity` to all subscribers.

```json
{
  "event": "simulators/init",
  "data": [{
    "id": "01234567-89ab-cdef-0123-456789abcdef",
    "config": {
      "method": "http/put",
      "path": "/resource/:id",
      "response": {
        "status": 201
      }
    }
  }]
}
```

#### `POST /api/simulators`

Add a simulator without destroying previously created simulators. Simulators must be unique on `METHOD` and `PATH`.
This request fails if it would step on an existing simulator.

```bash
$ curl -X 'POST' \
       -H 'Content-Type: application/json' \
       --data '{"simulator":{"method":"http/post","path":"/resource","response":{"status":201}}}' \
       http://localhost:3000/api/simulators
```

This sends a message via `ws://localhost:3000/api/simulators/activity` to all subscribers.

```json
{
  "event": "simulators/add",
  "data": {
    "id": "01234567-89ab-cdef-0123-456789abcdef",
    "config": {
      "response": {
        "status": 201
      },
      "method": "http/post",
      "path": "/resource"
    }
  }
}
```

#### `GET /api/simulators`

Get simulators with all stored activity.

```bash
$ curl http://localhost:3000/api/simulators
#=> {
#=>   "simulators": [{
#=>     "id": "01234567-89ab-cdef-0123-456789abcdef",
#=>     "config": {
#=>       "method": "http/put",
#=>       "path": "/resource/:id",
#=>       "response": {
#=>         "status": 201
#=>       },
#=>     "requests": [{
#=>       "id": "01234567-89ab-cdef-0123-456789abcdef",
#=>       "query-params": {
#=>         "q": "a-query"
#=>       },
#=>       "route-params": {
#=>         "id": "123"
#=>       },
#=>       "headers": {
#=>         "request-header": "value"
#=>       },
#=>       "body": "{\"some\":\"body\"}",
#=>       "timestamp": "1970-01-01T00:00:00Z"
#=>     }]
#=>   }
#=> }
```

#### `DELETE /api/simulators/reset`

Reset all simulators back to initial setup.

```bash
$ curl -X 'DELETE' http://localhost:3000/api/simulators/reset
```

This sends a message via `ws://localhost:3000/api/simulators/activity` to all subscribers.

```json
{
  "event": "simulators/reset-all",
  "data": [{
    "id": "01234567-89ab-cdef-0123-456789abcdef",
    "config": {
      "method": "http/post",
      "path": "/resource",
      "response": {
        "status": 201
      }
    },
    "requests": []
  }, {
    "id": "47c5d4b7-4602-40e5-966e-1a49e6c7db90",
    "config": {
      "response": {
        "status": 200
      },
      "method": "http/put",
      "path": "/resource/:id"
    },
    "requests": []
  }]
}
```

#### `WS /api/simulators/activity`

A web socket activity feed for all activity that modifies the state of the running simulators which includes CRUD
operations on and interactions with the simulators. Each message sent on the activity feed contains an `event` and
`data`.

The `event` will contain a `domain`, an optional `sub-domain`, and a `type` in the format `domain[.sub-domain]/type`.
Each distinct `event` will have a corresponding schema for its `data` model.

##### Event: `simulators/init`

Lorem ipsum. 

### HTTP Simulators

required vs optional|nullable
enum
string
non-neg integer
re-string
map
list
```json
{
  "method": "http/get|http/patch|http/put|http/post|http/delete",
  "path": /\/|(\/:?[a-z-0-9_])+/,
  "delay": [0-9]+|null,
  "response": {
    "headers": {
      
    } | null,
    "body"
  }
}
```

#### `{METHOD} /simulators/{simulator-path}`
#### `GET /simulators/{method}/{simulator-path}` or `GET /simulators/{simulator-id}`
#### `DELETE /simulators/{method}/{simulator-path}` or `DELETE /simulators/{simulator-id}`
#### `PATCH /simulators/{method}/{simulator-path}` or `PATCH /simulators/{simulator-id}`

### WS Simulators
#### `WS /simulators/{simulator-path}`
#### `GET /simulators/ws/{simulator-path}` or `GET /simulators/{simulator-id}`
#### `DELETE /simulators/ws/{simulator-path}` or `DELETE /simulators/{simulator-id}`
#### `PATCH /simulators/ws/{simulator-path}` or `PATCH /simulators/{simulator-id}`
#### `POST /simulators/ws/{simulator-path}` or `POST /simulators/{simulator-id}`
#### `POST /simulators/ws/{simulator-path}/:socket-id` or `POST /simulators/{simulator-id}/:socket-id`

### File Simulators
#### `{METHOD} /simulators/{simulator-path}`
#### `GET /simulators/{method}/{simulator-path}` or `GET /simulators/{simulator-id}`
#### `DELETE /simulators/{method}/{simulator-path}` or `DELETE /simulators/{simulator-id}`
#### `PATCH /simulators/{method}/{simulator-path}` or `PATCH /simulators/{simulator-id}`

### SMTP Simulator

### S3 Simulator

## Development

### Install

```bash
$ git clone git@github.com:skuttleman/clj-app-simulator.git
$ cd clj-app-simulator
$ lein install
```

### Run

Run locally on port `3000` with a server nrepl at port `7000` and figwheel nrepl at port `7888`.

```bash
$ lein cooper
```

You can optionally specify the server port and server nrepl port.

```bash
$ PORT=1234 REPL_PORT=4321 lein cooper
```


-- Hydrated render Pt II
-- finish README
-- CLJS tests from cli
-- SMTP Simulator
-- Elm ??
-- S3 ??
