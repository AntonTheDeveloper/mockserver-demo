### Как запустить MockServer в Docker

Читайте документацию: https://www.mock-server.com/mock_server/running_mock_server.html#docker_container

Мы будем использовать следующую команду для запуска MockServer в Docker:

```
docker run -d --rm --network="host" mockserver/mockserver
```

Обратите внимание на аргумент `--network="host"`. Мы делаем именно так, потому что некоторые запросы мы будем перенаправлять изнутри контейнера на адрес localhost c host-машины.
Знайте, что в режиме host-сети все пробрасываемые вами порты не действуют, т.е., порт 1080 изнутри контейнера (порт MockServer по-умолчанию) будет проброшен автоматически и, насколько я знаю, изменить вы это не можете. Если вы хотите запускать несколько отдельных MockServer контейнеров, вам придется менять в каждом MockServer контейнере внутренний порт через `-serverPort`, например: `docker run -d --rm --network="host" mockserver/mockserver -serverPort 1090`. Для целей текущей презентации, первой команды будет достаточно.

В результате, MockServer API будет доступно на http://localhost:1080

Дополнительно, будет поднят [mockserver-ui dashboard](https://github.com/mock-server/mockserver-ui) на http://localhost:1080/mockserver/dashboard

### Примеры мокирования напрямую через REST API (с помощью curl)

API MockServer очень богатое, у вас есть много возможностей соотнести и настроить поведение с каждым мокируемым запросом, смотрите API-документацию: https://app.swaggerhub.com/apis/jamesdbloom/mock-server-openapi/5.10.x#

Давайте замокируем все зависимые внешние запросы, для того чтобы запросы `curl -X POST http://localhost:8080/api/vote/yes` и `curl -X POST http://localhost:8080/api/vote/no` завершились успешно.

`http://localhost:1984/api/greeting` работает, поэтому его мокировать не надо, нужно просто его вызвать в том же виде, в каком запрос приходит в MockServer.
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
Следующий внешний запрос `localhost:1984/api/is-it-voting-day` будет отвечать значением `false`, если сегодня не один из дней из диапазона от `25.06.20` до `01.07.20`. А нам для успешности запросов `localhost:8080/api/vote/yes` и `curl -X POST http://localhost:8080/api/vote/no` требуется получить значение `true`. Давайте замокируем это значение.

Заметьте, что в данном случае нам приходится указывать заголовок `"content-type"`. 

MockServer не делает всю грязную работу вместо вас, вместо этого он дает вам полный контроль над настройкой поведения запросов. Вам придется вручную добавлять всю необходимую мета-информацию (например, заголовки) для того чтобы ваш фремворк мог сконвертировать ответ в некий тип.

Даже такие простые boolean-значения должны быть обработанны некоторым конвертером, если вы хотите получить в итоге соответствующий java boolean тип. В данном случае, вы можете испоьзовать json, потому что отдельные boolean значения являются валидными JSON типами согласно спецификации JSON.

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

Последний внешний запрос `localhost:1984/api/vote` работает в случае положительного голосования, но падает с исключением в случае отрицательного. Поэтому, при payload с положительным голосованием будем перенаправлять запрос, а для отрицательного будем мокировать возможный ответ. Заметьте насколько просто настроить различное поведение для одного и того же API на основе некоторых условий. В данном случае мы используем условие, реагирующее на определенный payload, но вы можете использовать также условие по заголовку, по cookie, по http-методу, по параметрам url и по самому url. Также, вы можете охватывать такими условиями несколько разных запросов, задавая им одинаковое поведение, например соотнося их по url с применением регулярных выражений.
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