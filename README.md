# fabulate

Fabulate generates data from your specifications. It could be used for filling a test system with dummy customers, items and orders, or just for fun.

## Usage

You can run some of the included samples using the lein shell script (from the [leiningen](https://github.com/technomancy/leiningen) tool for clojure project automation).

If we take a look at the file named "items.fab" in the samples folder, it contains the following;

	id			int [10000 99999] # a range of integers
	category	{Home Office Garden Construction Tools}	# a few possible choices
	price		price [10 1000]		# a range of doubles, rounded as price

	# a column may include the value of other columns
	blurb		format "Item %s is a nice %s product for only €%.2f" $id $category $price

You can then get Fabulate to generate items according to this specification. In the project directory run the command

	lein run samples/items.fab

Fabulate will now generate a number of sample items for you and output them to the console. For example;

	{:category "Garden", :id 65221, :price 548.6, :blurb "Item 65221 is a nice Garden product for only €548,60"}
	{:category "Home", :id 85426, :price 34.3, :blurb "Item 85426 is a nice Home product for only €34,30"}
	{:category "Tools", :id 37045, :price 131.8, :blurb "Item 37045 is a nice Tools product for only €131,80"}
	{:category "Construction", :id 67482, :price 626.6, :blurb "Item 67482 is a nice Construction product for only €626,60"}
	{:category "Tools", :id 37908, :price 58.6, :blurb "Item 37908 is a nice Tools product for only €58,60"}
	{:category "Garden", :id 19950, :price 818.4, :blurb "Item 19950 is a nice Garden product for only €818,40"}
	{:category "Garden", :id 29907, :price 434.4, :blurb "Item 29907 is a nice Garden product for only €434,40"}
	{:category "Construction", :id 90977, :price 492.5, :blurb "Item 90977 is a nice Construction product for only €492,50"}
	{:category "Office", :id 15681, :price 415.5, :blurb "Item 15681 is a nice Office product for only €415,50"}
	{:category "Garden", :id 13909, :price 53.2, :blurb "Item 13909 is a nice Garden product for only €53,20"}

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

## Development
When developing, one can also try;

	lein help
	lein kibit				run static code analysis (install kibit first)
	lein midje				run tests (once)
	lein midje :autotest	run tests (on file change)

## License

Copyright © 2013 Martin Hellspong

Distributed under the Eclipse Public License, the same as Clojure.
