# Invisible Queries RequestHandler
This is a Solr RequestHandler that allows to create a chain of SearchHandler references. 
Each chained handler is invoked in sequence only if the preceeding handler execution didn't produce any results (i.e. matches). 
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

and you want to run the first handler (/rh1), then the second (/rh2) if the first produces no results, and finally the third (/rh3) but only if the second didn't get any matches. 

Usually, this is a behaviour which belongs to the client application, but that assumes you have control on that client. 

Unfortunately there are some contexts where you don't have such kind of control; an example? The Magento-Solr connector is a blackbox where you configure something on Magento and Solr side, but of course, you cannot declare such "query workflow" behaviour like that described above: on top of a user query, the Magento connector executes a given search logic that you cannot change (unless you're are a Magento developer, of course)

So here comes this request handler: once plugged in Solr, you can chain the three handlers in this way: 

```xml
	<requestHandler name="/search" class="org.gx.labs.crh.FacadeRequestHandler">
		<str name="chain">/rh1,/rh2,/rh3</str>
	</requestHandler>
```

In this repository, other than the handler itself, you will also find a sample schema, a solrconfig plus unit / integration tests that demonstrate the behaviour.  

Enjoy!
