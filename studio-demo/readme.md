Welcome to the TypeDB IAM project demo!

To get started, try running the examples. They are intended
to be run once each and in order, so be aware that running
them more than once or out of order might generate data
errors. If anything goes wrong, you can run the
"reset-examples.tql" script to reset them, though this
will not reset other changes you make.

Once you've tried the pre-written examples out, have a go
at editing them or writing something yourself.

PRE-RELEASE INSTRUCTIONS FOR RUNNING:
Run the following command from the repo root to start the simulation:
`bazel run //simulation:run -- --database=typedb --config=./simulation/config/demo-setup.yml`
Once the simulation is complete, the examples can be run in order.