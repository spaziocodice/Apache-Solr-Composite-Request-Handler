# CompositeRequestHandler
This is a Solr RequestHandler that allows to create a chain of preexisting SearchHandler references.   
Each handler in the chain is associated with a rule which controls the transition to the next handler: when a given handler is invoked (i.e. its query is executed), the CompositeRequestHandler only if the preceeding handler execution didn't produce any results (i.e. matches). 
In this way you can configure something like a workflow, reusing the handlers you already configured in solrconfig.xml

For example, let's imagine we have the following three handlers defined:

```xml
	<!-- Request Handler #1: eDisMax on title with a rigid mm -->
	<requestHandler name="/rh1" class="solr.SearchHandler">
		<lst name="defaults">
			<str name="fl">id, [explain]</str>
			<int name="rows">10</int>
			<str name="defType">edismax</str>
			<str name="mm">100%</str>
			<str name="qf">title</str>
		</lst>
	</requestHandler>

	<!-- Request Handler #2: eDisMax on title with just one mandatory clause -->
	<requestHandler name="/rh2" class="solr.SearchHandler">
		<lst name="defaults">
			<str name="fl">title, [explain]</str>
			<str name="defType">edismax</str>
			<str name="mm">1</str>
			<str name="qf">title</str>
		</lst>
	</requestHandler>
	
	<!-- Request Handler #3: eDisMax on author name with maximum mm -->
	<requestHandler name="/rh3" class="solr.SearchHandler">
		<lst name="defaults">
			<str name="defType">edismax</str>
			<str name="fl">author,[explain]</str>
			<str name="qf">author</str>
			<str name="mm">100%</str>
			<str name="tie">0.01</str>
		</lst>
		<lst name="invariants">
		</lst>
	</requestHandler>
```

and you want to run the first handler (/rh1), then the second (/rh2) if the first produces more than one result, and finally the third (/rh3) but only if the second didn't get any matches. 

Usually, this is a behaviour which belongs to the client application, but this implicitly assumes you have control on the client code. 
Unfortunately there are some contexts where you cannot have such kind of control; you can think to a Magento-Solr integration scenario where the "client" part 
is hidden in Magento, and unless you are a Magento developer (and you are ok with changing the client code) you cannot change the built-in search workflow.

So here comes this request handler: once plugged in Solr, you can chain the three handlers above in this way: 

```xml
	<requestHandler name="/search" class="io.sease.crh.CompositeRequestHandler">
		<str name="chain">/rh1,/rh2,/rh3</str>
		<str name="rules">eq1,gt0,always</str>
	</requestHandler>
```

The first parameter is the chain composition, that is: the list of all request handlers that compose the chain. 
Following the same order of the chain, the "rules" parameter is a list the rules associated to each handler. At the moment the following rules are available: 

- eq: the rules matches (i.e. the subsequent handler in the chain won't be invoked) only if the current handler response produced exactly n results
- lt: the rules matches (i.e. the subsequent handler in the chain won't be invoked) only if the current handler response produced less than n results
- gt: the rules matches (i.e. the subsequent handler in the chain won't be invoked) only if the current handler response produced more than n results
- always: the rule always matches. This is usually the rule associated with the last handler in the chain 

Now, executing a query like this: 

```
> curl "http://127.0.0.1:8983/solr/example/search?q=Andrea"
```

will start the "/rh1, /rh2, /rh3" workflow defined above.

A maven repository contains the last stable version of the component: 

```xml
<repositories>
    <repository>
        <id>iqrh-mvn-repo</id>
        <url>https://raw.github.com/SeaseLtd/composite-request-handler/maven-repository</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>
```

After declaring the repository in your pom.xml you can get the artifact using the following coordinates:

```xml
<dependency>
	<groupId>io.sease</groupId>
	<artifactId>composite-request-handler</artifactId>
	<version>1.0</version>
</dependency>
```
