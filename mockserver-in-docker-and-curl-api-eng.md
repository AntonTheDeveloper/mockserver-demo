### How to run MockServer in Docker

Read doc: https://www.mock-server.com/mock_server/running_mock_server.html#docker_container

We will use the following command to run MockServer in Docker:

```
docker run -d --rm --network="host" mockserver/mockserver
```

Note the arg `--network="host"`. We want this because we'll forward some requests from inside container to localhost on host machine.
Be aware, that published ports are discarded when using host network mode, so, port 1080 (default MockServer port) will be exposed automatically and (AFAIK) you can not change it. You need to configure different MockServer ports (that is used inside) via `-serverPort` if you want to run multiple separate MockServer containers, for example ` docker run -d --rm --network="host" mockserver/mockserver -serverPort 1090`. But for the purpose of this demo this will be sufficient.

As the result, MockServer API will be available on http://localhost:1080

Additionally, [mockserver-ui dashboard](https://github.com/mock-server/mockserver-ui) will be available on http://localhost:1080/mockserver/dashboard

### Examples of mocking by direct rest api (via curl)

There are very rich MockServer API, you have many options to customize request matching and behavior, check API doc: https://app.swaggerhub.com/apis/jamesdbloom/mock-server-openapi/5.10.x#

Let's mock all dependent requests, in order to requests `curl -X POST http://localhost:8080/api/vote/yes` and `curl -X POST http://localhost:8080/api/vote/no` will be finished successfully.

`http://localhost:1984/api/greeting` is working, so we don't need to mock it, just forward such requests as is. 
```
curl -X PUT "http://localhost:1080/expectation" \
-H "accept: */*" -H "Content-Type: application/json" \
-d '{
  "httpRequest" : {
    "path" : "/api/greeting"
  },
  "httpForward": {
    "host": "localhost",
    "port": 1984,
    "scheme": "HTTP"
  }
}'
```

Next external request `localhost:1984/api/is-it-voting-day` will send response with `false`, if today is not the day in range from `25.06.20` to `01.07.20`. But, for success of `localhost:8080/api/vote/yes` we need response with `true`. Let's mock it.

Note, that we have to specify `"content-type"` header. MockServer doesn't do all dirty work for you, instead, it gives you full control over requests behavior definition. You have to add all necessary meta info (such as headers) manually, in order to your framework could convert responses to certain types.

Even such simple boolean values have to be processed throw some converter if you want it to be ended up in correspond java boolean type. In this case, you can use json, because single boolean values are valid JSON types by JSON specification.
```
curl -X PUT "http://localhost:1080/expectation" \
-H "accept: */*" -H "Content-Type: application/json" \
-d '{
  "httpRequest" : {
    "path" : "/api/is-it-voting-day"
  },
  "httpResponse" : {
    "statusCode": 200,
    "headers": {
        "content-type": ["application/json; charset=utf-8"]
    },
    "body" : "true"
  }
}'
```

Last external request `localhost:1984/api/vote` is working in case of `yes` vote, but it fails with exception in case of `no` vote. So, lest's match `yes` payload to proxy behavior, and for `no` - mock response. Note how easy to setup different behavior based on some conditions for the same API. Here we're using payload match condition, but you can also use matching of headers, cookies, queryStringParameters, paths and http-methods. Also, you can match multiple different requests with one single behavior setup, for example, through reg-exp in path of request.
```
curl -X PUT "http://localhost:1080/expectation" \
-H "accept: */*" -H "Content-Type: application/json" \
-d '{
  "httpRequest" : {
    "path" : "/api/vote",
    "body" : { "agree": true }
  },
  "httpForward": {
    "host": "localhost",
    "port": 1984,
    "scheme": "HTTP"
  }
}'
```

```
curl -X PUT "http://localhost:1080/expectation" \
-H "accept: */*" -H "Content-Type: application/json" \
-d '{
  "httpRequest" : {
    "path" : "/api/vote",
    "body" : { "agree": false }
  },
  "httpResponse" : {
    "statusCode": 200,
    "body" : "Please stay where you are and wait for further instructions"
  }
}'
```