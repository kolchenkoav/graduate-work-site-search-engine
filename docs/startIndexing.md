## Спецификация API

### Запуск полной индексации — GET /api/startIndexing

Метод запускает полную индексацию всех сайтов или полную переиндексацию, если они уже проиндексированы.  
Если в настоящий момент индексация или переиндексация уже запущена,  
метод возвращает соответствующее сообщение об ошибке.

**Параметры:**

Метод без параметров

**Формат ответа в случае успеха:**

{
'result': true
}

**Формат ответа в случае ошибки:**

{
'result': false,
'error': "Индексация уже запущена"
}
