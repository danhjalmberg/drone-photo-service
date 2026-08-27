# Project Evolution

This document summarizes the development of the Drone Photo Service from the
original university submission to the current implementation. It focuses on the
main problems that motivated later work and the direction of the resulting
refactoring rather than describing individual implementation changes.

The current structure of the application is documented separately in the
Architecture document, while significant architectural and design choices are
explained in the Architecture and Design Decisions document. This document
provides the broader development context connecting those decisions to the
original project.

## Contents

1. [Original University Project](#original-university-project)
2. [MVC Refactoring](#mvc-refactoring)
3. [Simulation and Timing Refactoring](#simulation-and-timing-refactoring)
4. [UI and Visualization Development](#ui-and-visualization-development)
5. [Validation and Error Handling](#validation-and-error-handling)
6. [Testing and Reliability](#testing-and-reliability)
7. [Lifecycle and Concurrency Review](#lifecycle-and-concurrency-review)
8. [Documentation and Portfolio Finalization](#documentation-and-portfolio-finalization)

<hr>

## Original University Project

The Drone Photo Service originated as the final project for a university course
in object-oriented programming. The assignment was intended primarily to
demonstrate understanding of the concepts covered by the course rather than to
produce a production-oriented application. Its requirements included an
event-driven Swing interface, concurrent processing and synchronization,
Model-View-Controller architecture, several specified design patterns, and use
of the Streams API. Higher grades required progressively broader demonstrations
of these concepts.

The application concept was developed around these requirements. Photo agencies
act as producers that create photo, video, and zoom tasks, while drones act as
consumers that retrieve tasks from a shared bounded queue. Drones move across an
aerial map, manage battery state, execute the assigned task, and return
completed results to the originating agency. Different drone configurations are
assembled from battery, motor, and camera components, and captured map imagery
is processed to produce color, grayscale, negative, video, and zoom results.

This domain provided a natural context for several course requirements. Photo
agencies and drones could execute concurrently, the shared task queue provided a
synchronization point for producer-consumer behavior, and drone construction
provided applications for factory-related patterns. Swing components and layouts
were used to present simulation state, map positions, and captured imagery. The
resulting project therefore combined concurrency, object composition, design
patterns, image processing, and graphical presentation in a single working
simulation.

The original implementation was organized around one `Model`, one `View`, and
one `Controller`. The model contained simulation state together with map and
image processing, actor collections, executor services, task archival, and file
operations. The view constructed the Swing interface and displayed model data.
The controller handled commands, simulation control, view updates, map loading,
and communication between the other two parts of the application.

Although this organization provided a recognizable MVC structure, the boundaries
were not consistently maintained. Some mutable model state had wider visibility
than necessary, and model objects were passed directly to view code. The
submitted implementation therefore did not fully satisfy the assignment's
requirement that the model and view remain completely decoupled and communicate
through the controller. These issues were specifically identified in the
original assessment.

The original application also contained several functional and lifecycle
limitations. Evaluation identified inconsistent behavior between some task
types, problems around pause and resume, and simulation state from previous runs
remaining visible after a new simulation was started. Image export also depended
on assumptions about the repository and runtime directory structure, with an
undesirable fallback to the user's home directory when the expected destination
could not be resolved.

Despite these limitations, the original project established most of the domain
concepts that remain in the current application. Photo agencies, drones, tasks,
the producer-consumer queue, configurable drone components, map-based movement,
image capture, and the three task types were already present. The assessment
confirmed that the project met the highest specified requirements for concurrent
processes, synchronization, Swing component and layout variety, and the required
design patterns. The application was therefore retained as the foundation for
further development rather than replaced with a new project.

Later development followed a different objective. Instead of extending the
application primarily to demonstrate additional language features or design
patterns, the existing implementation was treated as a software system to be
reviewed and improved. Features and structures that remained useful were
preserved, while responsibility boundaries, simulation behavior, lifecycle
management, presentation, validation, error handling, and testability were
reconsidered where the original implementation exposed weaknesses.

This change of purpose also changed the role of the documentation. The original
project report concentrated heavily on describing the resulting implementation.
The assessment noted that it provided insufficient explanation of why classes
and logic had been designed as they were, which problems had arisen during
development, and how those problems had been resolved. The subsequent project
documentation therefore records not only the resulting architecture but also the
reasoning and evolution that produced it.

<hr>

## MVC Refactoring

The first major architectural review concerned the application's
Model-View-Controller structure. Although the original project was organized
around a `Model`, `View`, and `Controller`, their boundaries were not
consistently maintained. The controller had accumulated most application
workflows, while the model combined simulation state with several infrastructure
responsibilities. Mutable domain objects and collections were also passed
directly to the view.

The existing MVC architecture was retained rather than replaced. Application
construction was moved out of the controller, and the root controller was
progressively reduced to composition and coordination. Operational
responsibilities such as simulation control, view refresh, map loading,
playback, selection, and image export were separated into specialized
controllers.

The model-view boundary was refined in parallel. Mutable simulation objects were
kept within the model and replaced at the presentation boundary by read-only
snapshots and view-specific data. Controllers became responsible for retrieving
model state and preparing the representations required by the view.

The result was not a change of architectural pattern, but a stricter application
of the original MVC design. The model owns simulation state and domain behavior,
the view owns graphical presentation, and the controller layer provides the
communication and coordination between them.

<hr>

## Simulation and Timing Refactoring

The original simulation combined periodic execution with wall-clock time, with
some simulation behavior tied to the execution of the concurrent actors. Drone
movement, battery behavior, task timing, and other temporal state were therefore
partly dependent on how frequently the corresponding work was scheduled and how
quickly the application executed. Pause and resume behavior also exposed
weaknesses in this approach because stopping execution did not provide a single,
consistent definition of simulated elapsed time.

The timing model was refactored around an explicit simulation clock and a
centralized physics update. Simulation time advances by defined time steps, and
time-dependent domain behavior uses this shared temporal reference instead of
deriving elapsed time from the operating system. Graphical refresh was separated
from simulation progression so that presentation frequency no longer determines
the rate at which the simulated world advances.

This separation also made simulation speed a property of the simulation rather
than of processor or rendering performance. Pausing freezes simulated time,
while movement, battery consumption, task timestamps, processing durations, and
event timestamps remain based on the same temporal model. Wall-clock time was
retained only where real calendar time is required outside the simulation, such
as naming exported files.

The refactoring established a more deterministic foundation for later changes.
Simulation behavior could be advanced explicitly in tests, event history could
use meaningful simulation timestamps, and concurrency could be reviewed
separately from temporal progression. Timing consequently changed from an
implementation detail of the execution mechanism into an explicit part of the
simulation model.

<hr>

## UI and Visualization Development

The original interface provided the essential controls and visual output
required by the university project, including simulation settings, textual
status information, the map, and captured task imagery. As the application was
retained beyond the original assignment, the interface increasingly became a
tool for observing and inspecting the simulation rather than only demonstrating
that it was running.

The layout was reorganized into more focused areas for simulation controls, map
visualization, status information, actor monitoring, task results, and detailed
inspection. Selection was made consistent across the map, task thumbnails, and
details panels, while completed tasks gained richer visualization of capture
positions, video paths, and generated image sequences. Event and actor monitors
were added to expose important simulation activity without mixing diagnostic
output into the domain model.

These changes also reinforced the MVC refactoring. Swing components no longer
receive live domain objects for rendering; controllers prepare snapshots and
presentation-specific data instead. The interface consequently observes the
simulation through a presentation boundary rather than participating in the
simulation's state progression.

The resulting interface retains the desktop simulation character of the original
project but presents its behavior more explicitly. UI development was therefore
used primarily to make existing domain behavior, state transitions, task
results, and simulation history observable rather than to expand the underlying
feature set.

<hr>

## Validation and Error Handling

The original application handled invalid input and operational failures largely
where they occurred. Validation was limited in several paths, exceptions were
sometimes handled through console output or stack traces, and failure behavior
was not consistently separated between the model, controller, and view. This
made it difficult to determine which layer was responsible for rejecting invalid
state and for communicating failures to the user.

Validation was progressively moved to explicit boundaries. User input is checked
before it enters the simulation where appropriate, while model classes validate
the invariants they own independently of the interface. File and metadata
loading similarly validate external data before it is committed to model state,
so failed operations do not leave partially updated state.

Error propagation was separated from error presentation. Lower-level code
reports failures through exceptions rather than deciding how they should be
displayed, while controllers catch failures at user-operation boundaries and
translate them into appropriate view messages. Unexpected technical failures can
therefore be reported without coupling model and utility code to Swing dialogs.

The resulting approach treats validation and error handling as part of the
application boundaries rather than as isolated defensive checks. Invalid state
is rejected close to the component that defines the corresponding invariant,
while controllers determine how failures affect the current workflow and what
the user should see.

<hr>

## Testing and Reliability

The original university project relied primarily on manual execution and
observation of the application. This was sufficient for demonstrating the
required functionality, but provided little regression protection as the
architecture and simulation behavior were subsequently refactored.

Automated tests were introduced progressively around behavior that could be made
deterministic and isolated from Swing rendering and thread scheduling. The suite
grew to cover domain calculations, coordinate conversions, map and metadata
handling, drone behavior, simulation time, task storage, validation, image
processing, event history, and important failure conditions.

Testing also influenced the implementation itself. Explicit simulation time,
controlled randomness, temporary file resources, clearer model invariants, and
more focused component boundaries made important behavior reproducible without
running the complete application. Failure-oriented tests were used not only to
verify that invalid operations are rejected, but also that previously valid
state is preserved where required.

The resulting test suite is intentionally selective rather than exhaustive.
Deterministic model and support behavior receives automated regression coverage,
while Swing presentation, visual quality, and timing-dependent behavior of the
complete concurrent simulation remain primarily subject to application-level
verification. Testing therefore became part of the project's reliability
strategy without expanding into comprehensive GUI or concurrency automation.

<hr>

## Lifecycle and Concurrency Review

Concurrency was already central to the original project through independently
executing photo agencies and drones and their shared producer-consumer queue.
This execution model was chosen primarily for pedagogical reasons: representing
the simulation actors as concurrent workers provided a concrete demonstration of
Java concurrency, synchronization, and the producer-consumer pattern required by
the university assignment. It was not intended as a strategy for distributing
CPU workload or as a scalable one-thread-per-entity architecture.

Retaining this educational concurrency model exposed an important distinction
during later development. Simulated movement and other time-dependent behavior
should not depend on how frequently individual actor threads happen to execute,
because thread scheduling is affected by the host system and does not provide a
reliable simulation clock. Later development therefore focused less on adding
concurrency than on separating actor execution from simulation progression and
making thread ownership, timing, and lifecycle behavior more explicit.

Thread responsibilities were separated more clearly as the simulation was
refactored. Centralized physics updates became responsible for advancing
simulation state, actor threads retained asynchronous work that should not block
that progression, and Swing updates remained on the Event Dispatch Thread.
Bounded queues and executor resources were used to prevent background work from
accumulating without control.

Application lifecycle operations were reviewed together with this execution
model. Starting, pausing, resuming, stopping, resetting, and closing the
application were given explicit responsibilities, including cancellation and
shutdown of scheduled work and actor executors. Resetting model state is delayed
until the resources operating on that state have terminated.

The resulting concurrency model preserves the producer-consumer character of the
original project while defining clearer ownership and shutdown boundaries.
Concurrency is treated as part of the application lifecycle rather than only as
a requirement for actors to execute in parallel.

<hr>

## Documentation and Portfolio Finalization

As the major architectural and reliability issues were addressed, the focus of
development shifted from extending the application to completing it as a
portfolio project. New functionality was deliberately limited unless required to
correct a defect or clarify an existing feature.

Documentation became part of this finalization process. The current architecture
was documented separately from the significant decisions that produced it, while
the project evolution records the main changes from the original university
submission. This addresses a weakness of the original report, which described
the resulting implementation in greater detail than the reasoning and
development process behind it.

Source documentation was reviewed across the application, package-level
Javadocs were added, and Maven reporting was configured to generate HTML API
documentation. A repository-specific Checkstyle policy was also introduced and
integrated into the normal `verify` build so the adopted source conventions are
reproducible rather than dependent on IDE settings.

The remaining work focuses on presenting and maintaining the completed project:
a concise project README, architecture diagrams, selected screenshots and
animations, reproducible build instructions, and final repository cleanup. The
objective is to make the existing engineering work understandable and
demonstrable without continuing to expand the scope of the application.
