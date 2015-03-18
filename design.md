# Designing Parmenides

This document details the thinking that is driving the design of
Parmenides. Parmenides is a general catch all name for everything related to the
various systems proposed to solve the entity resolution and information quality
problems that we find ourselves with when dealing with messy government
data. Parmenides isn't tied to any programming language, software, or technique.

## Two sentence background statement

Records in government data don't come with reliable identifiers attached
indicating which records represent the same entity. Lacking reliable
identifiers, results from analysis using government data are mostly meaningless. 

## Simple singular example

Form A refers to a person with the name "Bob S. Smith" who works for
"ACME Co.", while Form B refers to a person with the name "Robert Smth" who
works for "acme inc.". There is no other information. Are these the same person? 

## Problem question

How do we build a system that allows us to find, manage and distribute our
understanding of which government records represent the same entities?

## Constraints

Based on research and previous experiences, here are some hard constraints on
the solution.

### Too much data to do by hand

We are interested in a wide variety of government datasets. The largest
collection we've seen so far has several million records though. It's taken
other organizations decades to collate and reconcile these records
accurately. We don't have the time or manpower to replicate those
efforts. Moreover, we aim to integrate more datasets at a much faster pace than
other organizations have achieved thus far. Thus, we will be developing software
to automatically find matches in the data. 

### Human arbitration is sometimes necessary

There are instances when computers cannot determine whether two records should
be linked or when the computer links two records that, to a human, are obviously
the same. Parmenides must allows manual linking and unlinking of records. So, a
web ui at some point with general access to all the data. 

### Real time/streaming

We aren't only interested in the past, but rather any changes to the data as
time goes on. That means that every step of the reconciliation process (loading,
reconciliation, distribution, and manual editing) must be allowed to happen in
any order and still be correct.

### No Oracles/No Confidence/Infinite Doubt

In general, we will never know if we are right about our deductions and
reconciliations. Based on the data we have, it's simply not possible to have
confidence that we are exactly right. That doesn't mean that the results of
Parmenides aren't useful, but rather Parmendies must be humble and allow for
effective reversal of any actions taken. Unlinking and subsequent relinking of
records need to be possible.

### Multiple data sources

While there is a good deal of value in linking data within a data set, linking
across data sets is much more valuable. However, most pairs of datasets do not
have the same schema. This schema mismatch slows down and provides constant
friction during the process.

### Messy data sources

Data is messy. This can mean a variety of things, but mostly it means that each
data source requires a fair amount of work before record linkage techniques can
effectively be applied.

### Multiple data consumers

As the data gets more voluminous and the reconciliation techniques get better,
there will be a greater demand for the ability to mix and match different sets
of data. This means that Parmenides should support pulling down any of the silos
of data at any time. 

### Network/graph data

Organizations and people are well represented by graphs. Graphs don't play well
with tabular data stores like SQL yet and so special databases/program must be
used to work wit the graphs. 

### Diminishing returns from techniques

Exact stringing matching cannot be beat in terms of the ease of use, reliability
of results and speed. Every other technique provides orders of magnitude less
resolution ability if you do exact string matching first. The constraint here
then is that it might be better to be able to scale up exact string matching to
large numbers of records than to try and scale up complicated but more effective
techniques for a smaller number of records.

### Virtualization hasn't been shown to work

Entity resolution and record linkage techniques have weird data access patterns
that aren't terribly predictable. Industry folks have found that it is easier to
load all the data into one central repository (materializatoin) than it is for
one central program to reach out to dozens of different databases
(virtualization) during the record linkage process. Storage is cheap, but
developer time isn't. 

### Ephemeral datasets 

[TODO] Allows for continuous searching.

### Secretive industry / Murky past

After 9/11, the industry went dark and became much less open (or so I've
heard). Anti-terrorism efforts like Palantir and Infoglide (TSA) are under
similar constraints as we are (with some modifications like privacy, more
volume, and hard real time guarantees) trying to solve the same problem.

### Base Rate Fallacy

[TODO] More info for this

## Design abstractions

### Sets of records

### Equivalence relations

### Generalized link and unlink functions
Swoosh 
