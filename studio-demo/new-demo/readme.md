## Welcome to the TypeDB IAM project demo!

### Getting started

This demo assumes some familiarity with TypeDB Studio. If you have never used Studio before, you are strongly advised to
visit the documentation (link to docs).

Make sure you are on a `schema-write` transaction and run the following scripts:
- `../../iam-schema-core-concepts.tql`
- `../../iam-schema-core-rules.tql`
- `../../iam-schema-demo-ext.tql`

(These files will be moved into the demo directory in the final version.)

Then switch to a `data-write` transaction and run the following:
- `insert-demo-data.tql`

Remember to click on the green tick after running each of these scripts to commit the changes to the database.

### Running the examples

To get started, try running the examples. They are intended to be run once each and in order, so be aware that running
them more than once or out of order might generate data errors. If anything goes wrong, you can run the
`insert-demo-data.tql` script again to reset everything. All the examples use `data` sessions, but you'll have to switch
between `read` and `write` transactions depending on the queries in the example, and remember to commit after writes.
Each example has an accompanying exercise, but you can skip them out and all the examples will still run fine. Some are
much harder than others! All the solutions are in the `solutions.tql` file.

### Next steps

Once you've tried the pre-written examples out, have a go at editing them or writing something yourself. The schema also
has a lot of types that are not used in the example dataset, so try experimenting with those. Remember you can view the
list of types in the Type Browser, or view the schema graph by running the query `match $t sub thing;`.