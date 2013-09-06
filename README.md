# fabulate

Fabulate generates data from your specifications. It could be used for filling a test system with dummy customers, items and orders, or just for fun.

## Usage

If you create a file named "items.fab" with the following contents;

	id			int [10000 99999]
	category	{Home Office Garden Construction Tools}
	price		[10 1000]
	info		format "Item %s is a nice %s product for only €%.2f" $id $category $price

and then in the project directory run

	lein run samples/items.fab

Fabulate will generate a number of sample items for you and output them to the console. For example;

	{:category "Home", :id 73376, :price 987.2236925231873, :info "Item 73376 is a nice Home product for only €987,22"}
	{:category "Office", :id 82799, :price 693.8743376354212, :info "Item 82799 is a nice Office product for only €693,87"}
	{:category "Office", :id 42663, :price 486.0891496975339, :info "Item 42663 is a nice Office product for only €486,09"}
	{:category "Tools", :id 74508, :price 468.62396377542564, :info "Item 74508 is a nice Tools product for only €468,62"}
	{:category "Home", :id 92147, :price 925.4129631705134, :info "Item 92147 is a nice Home product for only €925,41"}
	{:category "Office", :id 94866, :price 203.67187669436308, :info "Item 94866 is a nice Office product for only €203,67"}
	{:category "Construction", :id 84127, :price 809.5594030051212, :info "Item 84127 is a nice Construction product for only €809,56"}
	{:category "Garden", :id 69834, :price 643.7863843402524, :info "Item 69834 is a nice Garden product for only €643,79"}
	{:category "Office", :id 26215, :price 253.7947513759061, :info "Item 26215 is a nice Office product for only €253,79"}
	{:category "Office", :id 66687, :price 573.2982294517358, :info "Item 66687 is a nice Office product for only €573,30"}

File output with formatting support is planned next

## Development
When developing, one can also try;

	lein help
	lein kibit				run static code analysis (install kibit first)
	lein midje				run tests (once)
	lein midje :autotest	run tests (on file change)

## License

Copyright © 2013 Martin Hellspong

Distributed under the Eclipse Public License, the same as Clojure.
