
[Постановка задачи](docs/Task.md)
# Разработка локального поискового движка по сайту

## Описание проекта
Разработано приложение, которое позволяет индексировать страницы и осуществлять по ним быстрый поиск.  
Движок разработан на фреймворке Spring. В проекте применены знания по ООП, коллекциям, работе с файлами и сетью,  
работе с базами данных и многопоточности. Мало веб-разработки и много алгоритмического кода.

#### Принципы работы поискового движка

1. В конфигурационном файле перед запуском приложения задаются адреса сайтов, по которым движок должен осуществлять поиск.
2. Поисковый движок должен самостоятельно обходить все страницы заданных сайтов и индексировать их (создавать так называемый индекс) так, чтобы потом находить наиболее релевантные страницы по любому поисковому запросу.
3. Пользователь присылает запрос через API движка. Запрос — это набор слов, по которым нужно найти страницы сайта.
4. Запрос определённым образом трансформируется в список слов, переведённых в базовую форму. Например, для существительных — именительный падеж, единственное число.
5. В индексе ищутся страницы, на которых встречаются все эти слова.
6. Результаты поиска ранжируются, сортируются и отдаются пользователю.



### стэк используемых технологий
Java 17  
Spring:  
* Lombok - Java annotation library which helps to reduce boilerplate code.
* Spring Web - Build web, including RESTful, applications using Spring MVC. Uses Apache Tomcat as the default embedded container.
* Spring Data JPA - Persist data in SQL stores with Java Persistence API using Spring Data and Hibernate.
* MySQL Driver - MySQL JDBC driver.
* morph, morphology, dictionary-reader - org.apache.lucene.morphology (org.apache.lucene.analysis) Поиск лемм
* Jsoup Java HTML Parser - soup is a Java library that simplifies working with real-world HTML and XML
* Guava: Google Core Libraries For Java - Guava is a suite of core and expanded libraries that include utility classes, 
  Google's collections, I/O classes, and much more.
* JMH Core - The jmh is a Java harness for building, running, and analysing nano/micro/macro benchmarks 
  written in Java and other languages targeting the JVM

### инструкцию по локальному запуску проекта — последовательность команд и действий
В коммандной строке набрать ' java -jar SearchEngine-1.0-SNAPSHOT.jar'





