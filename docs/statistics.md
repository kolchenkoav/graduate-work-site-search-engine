### Статистика — GET /api/statistics

Метод возвращает статистику и другую служебную информацию о состоянии поисковых индексов и самого движка.  
Если ошибок индексации того или иного сайта нет, задавать ключ error не нужно.

**Параметры:**

Метод без параметров.

**Формат ответа:**

{  
'result': true,  
'statistics': {  
"total": {  
"sites": 10,  
"pages": 436423,  
"lemmas": 5127891,  
"indexing": true  
},  
"detailed": [  
{  
"url": "http://www.site.com",  
"name": "Имя сайта",  
"status": "INDEXED",  
"statusTime": 1600160357,  
"error": "Ошибка индексации: главная  
страница сайта недоступна",  
"pages": 5764,  
"lemmas": 321115  
},  
...  
]  
}  
