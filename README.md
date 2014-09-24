# fabulate

Fabulate generates data from your specifications. It could be used for filling a test system with dummy customers, items and orders, or just for fun.

## Usage

You can run some of the included samples using the lein shell script (from the [leiningen](https://github.com/technomancy/leiningen) tool for clojure project automation).

If we take a look at the file named "items.fab" in the samples folder, it contains the following;

	id			/[A-Z]{2}\d{3}-\d{2}[a-z]/ 	# a value matching the specified regex
	category	<Home Office Garden Construction Tools>	# a few possible choices
	price		price [10 1000]		# a range of doubles, rounded as price

	# a column may include the value of other columns
	blurb		format "Item %s is a nice %s product for only €%.2f" $id $category $price

You can then get Fabulate to generate items according to this specification. In the project directory run the command

	lein run -i samples/items.fab csv

Fabulate will now generate a number of sample items for you in csv-format, and output them to the console. For example;

	id,category,price,blurb
	HP500-68q,Office,844.5,"Item HP500-68q is a nice Office product for only €844,50"
	WO299-02g,Office,188.3,"Item WO299-02g is a nice Office product for only €188,30"
	OW828-76x,Construction,735.7,"Item OW828-76x is a nice Construction product for only €735,70"
	TM275-55y,Tools,234.1,"Item TM275-55y is a nice Tools product for only €234,10"
	XZ777-90i,Home,212.6,"Item XZ777-90i is a nice Home product for only €212,60"
	CN335-82r,Home,670.0,"Item CN335-82r is a nice Home product for only €670,00"
	IH006-11i,Garden,936.5,"Item IH006-11i is a nice Garden product for only €936,50"
	GJ371-06k,Garden,360.5,"Item GJ371-06k is a nice Garden product for only €360,50"
	UM487-82y,Home,731.6,"Item UM487-82y is a nice Home product for only €731,60"
	UL110-77u,Tools,494.9,"Item UL110-77u is a nice Tools product for only €494,90"

If you'd rather output the CSV-data to a file, you can do that as well, see the command line reference.

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

Angle brackets indicate possible choices.

	<Red Green Blue>	# one of these three colors will be chosen, equally likely.

Individual choices can also be weighted, if they are to be more or less likely.

	<Red:3 Green:20 Blue:7>		# one of these three colors will be chosen, according to their weight.

Adding these weights now makes Green the most likely choice (probability 20/30th or about 66%), followed by Blue (7/30th, or about 23%) and then Red (3/30th or 10%). Choices without weights are considered to have a weight of one.

### Ranges

Square brackets indicate ranges of possible numeric values.

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

## Command Line Reference
Basic syntax is;

	lein run <generic-params> <writer> <writer-params>

An example would be 

	lein run -i samples/items.fab csv --separator=/

There are a few generic parameters valid for all writers

	-n 		--count ROWS	Number of rows to generate
	-i 		--input FILE 	Input file with fab column specifications
	-s 		--select FIELDS Comma separated list of field to include in the output (default: all)

And each writer has it own set of parameters. For now *csv* is the only available writer.

Be warned that error reporting from the command line parser is basically nonexistent. Also, when specifying an unknown writer name, you get a long error message with a stack trace, and the program exits. This will improve at some point in the future.

## Writers
A writer is a component that knows how to output the generated data in a specific way, such as a specific file format, or connecting to a specific database.

### The CSV writer
The CSV writer formats data according to the the [RFC4180](http://tools.ietf.org/html/rfc4180) specification (using the [clojure.data.csv](https://github.com/clojure/data.csv) library.)

The csv writer has the following additional parameters, that need to go after the name of the writer.

	-d 		--destination FILE 	Destination file (defaults to outputting data to the console)
	-s 		--separator CHAR	Field separator to use

If you cannot get the field separator to work as you expect, please be warned that some characters are treated differently by your shell such as semicolon, question mark, asterisk or backslash and may need to be escaped or quoted in order to become a valid parameter value, depending on your specific shell.

Note that at the present time the short argument name "-s" is used both as a generic argument (for selecting particular fields to output) and in the CSV writer to indicate the desired separator character. This works only because you cannot give generic arguments after the writer name argument, or writer-specific arguments before the writer name, so specifying

	lein run -i samples/items.fab -s id,price csv -s:

may indeed be confusing to the user, but is perfectly unambiguous to the command line parser.

## Development
This just serves to remind you that when developing, one can also try;

	lein help
	lein kibit				run static code analysis
	lein ancient			check for newer versions of project dependencies
	lein midje				run tests (once)
	lein midje :autotest	run tests (on file change)

You'll need to install the relevant lein plugins, of course.
### Instaparse

Fabulate's parser is built with the excellent [instaparse parsing library](https://github.com/Engelberg/instaparse).

## License

Copyright © 2013, 2014 Martin Hellspong

Distributed under the Eclipse Public License, the same as Clojure.
