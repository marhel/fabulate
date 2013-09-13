# fabulate

Fabulate generates data from your specifications. It could be used for filling a test system with dummy customers, items and orders, or just for fun.

## Usage

You can run some of the included samples using the lein shell script (from the [leiningen](https://github.com/technomancy/leiningen) tool for clojure project automation).

If we take a look at the file named "items.fab" in the samples folder, it contains the following;

	id			/[A-Z]{2}\d{3}-\d{2}[a-z]/ 	# a value matching the specified regex
	category	{Home Office Garden Construction Tools}	# a few possible choices
	price		price [10 1000]		# a range of doubles, rounded as price

	# a column may include the value of other columns
	blurb		format "Item %s is a nice %s product for only €%.2f" $id $category $price

You can then get Fabulate to generate items according to this specification. In the project directory run the command

	lein run samples/items.fab

Fabulate will now generate a number of sample items for you and output them to the console. For example;

	{:category "Office", :id "SK505-03z", :price 776.0, :blurb "Item SK505-03z is a nice Office product for only €776,00"}
	{:category "Tools", :id "IF633-18p", :price 957.6, :blurb "Item IF633-18p is a nice Tools product for only €957,60"}
	{:category "Tools", :id "XV045-58s", :price 21.3, :blurb "Item XV045-58s is a nice Tools product for only €21,30"}
	{:category "Construction", :id "UA035-16c", :price 687.3, :blurb "Item UA035-16c is a nice Construction product for only €687,30"}
	{:category "Garden", :id "RY150-55j", :price 831.5, :blurb "Item RY150-55j is a nice Garden product for only €831,50"}
	{:category "Home", :id "WI752-64q", :price 289.9, :blurb "Item WI752-64q is a nice Home product for only €289,90"}
	{:category "Office", :id "EZ888-04h", :price 345.7, :blurb "Item EZ888-04h is a nice Office product for only €345,70"}
	{:category "Tools", :id "OF871-65e", :price 570.2, :blurb "Item OF871-65e is a nice Tools product for only €570,20"}
	{:category "Home", :id "HR467-46b", :price 604.8, :blurb "Item HR467-46b is a nice Home product for only €604,80"}
	{:category "Construction", :id "GT654-47z", :price 459.1, :blurb "Item GT654-47z is a nice Construction product for only €459,10"}

(File output with formatting support is planned next)

## Syntax

The Fabulate data generation DSL currently supports the following constructs:

### Comments

	# Anything on a line after the comment sign # is ignored until the end of the line.

### Numeric and String Literals
A literal is a value representing itself. Simple words need not be quoted. Examples are:

	nice			# the string "nice"
	"very nice"		# wrap the string in quotes to include spaces
	123.45  		# the value 123.45

### Multiple choice

Curly brackets indicate possible choices.

	{Red Green Blue}	# one of these three colors will be chosen, equally likely.

Individual choices can also be weighted, if they are to be more or less likely.

	{Red:3 Green:20 Blue:7}		# one of these three colors will be chosen, according to their weight.

Adding these weights now makes Green the most likely choice (probability 20/30th or about 66%), followed by Blue (7/30th, or about 23%) and then Red (3/30th or 10%). Choices without weights are considered to have a weight of one.

### Ranges

Square brackets indicate ranges of possible values.

	[1 10]	# yields any decimal value between 1 (inclusive) and 10 (exclusive), equally likely, such as 4.56362721652

Weights can also be attached to the range endpoints, in order to make values from one end of the range more likely than the other.

	[1:2 10:8] # yields a decimal value between 1 (inclusive) and 10 (exclusive), but more likely towards the higher end.

The probability for getting the value 1 is not 2/10th however. In order to understand the probabilities for weighted ranges, consider the area under the line segment (1, 2) - (10, 8), which can be seen as a 2 by 9 rectangle, and a right angled triangle with sides (10-1)=9 and (8-2)=6. Intuitively we see that a larger part of the area of such a shape is near x=10 than at x=1, and so Fabulate will be more likely to pick a value near 10. 

### Complex ranges

Once we allow weighted ranges, it makes sense to allow ranges with more than two steps.

	[1:2 10:8 12:1]

This indicates two line segments (1, 2) - (10, 8), and (10, 8) - (12, 1). The area under the second line segment is much smaller than the area under the first. This means a higher probability that a value will be chosen from the first part of the range, weighted corresponding to their areas. Within the second range, values are more likely picked near the start of the range (10) than near the end (12), as the start carries a greater weight.

### Regular expressions

In Fabulate, regexes are not used to match text, instead they are used to generate data matching the regex, using the [re-rand library](https://github.com/weavejester/re-rand). Regexes are written between slashes, like JavaScript regex literals.

	/[A-Z]\d{3}/	# generates values like A637, H762, T013
	/[A-F0-9]{8}(-[A-F0-9]{4}){3}-[A-F0-9]{12}/	# generates a GUID-like value

## Development
When developing, one can also try;

	lein help
	lein kibit				run static code analysis (install kibit first)
	lein midje				run tests (once)
	lein midje :autotest	run tests (on file change)

### Instaparse

Fabulate's parser is built with the excellent [instaparse parsing library](https://github.com/Engelberg/instaparse).

## License

Copyright © 2013 Martin Hellspong

Distributed under the Eclipse Public License, the same as Clojure.
