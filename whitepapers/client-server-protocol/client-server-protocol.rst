#################################################
Google Wave Data Model and Client-Server Protocol
#################################################

:Authors: 
    Jochen Bekmann, 
    Michael Lancaster, 
    Soren Lassen, 
    David Wang 

:Version: 1.0 - May 2009

This whitepaper is part of a series. All of the whitepapers
can be found on `Google Wave Federation Protocol site`_.

.. _Google Wave Federation Protocol site: http://www.waveprotocol.org/whitepapers

Introduction
############

This document describes the Google Wave data model and the protocol by which a
wave client communicates with a wave server in order to create, read, and
modify waves. 

Data Model
##########

Wave Data Model
===============

Wave
  Each wave has a globally unique wave ID and consists of a set of wavelets.

Wavelet
  A wavelet has an ID that is unique within its containing wave and is composed
  of a participant list and a set of documents. The wavelet is the entity to
  which Concurrency Control / Operational Transformations apply.

Participant
  A participant is identified by a wave address, which is a text string in the
  same format as an email address (local-part@domain). A participant may be a
  user, a group or a robot. Each participant may occur at most once in the
  participant list.

Document
  A document has an ID that is unique within its containing wavelet and is
  composed of an XML document and a set of "stand-off" annotations. Stand-off
  annotations are pointers into the XML document and are independent of the XML
  document structure. They are used to represent text formatting, spelling
  suggestions and hyper-links. Documents form a tree within the wavelet.  There
  are currently two types of documents: text documents, used to represent
  the rich text messages in a wavelet (casually known as blips), and data
  documents which are typically invisible to the user (for example, tags). For
  detailed information on the XML structure of documents, please refer to the
  Google Wave Operational Transformation paper.

Wave view
  A wave view is the subset of wavelets in a wave that a particular user has
  access to. A user gains access to a wavelet either by being a participant on
  the wavelet or by being a member of a group that is a participant (groups may
  be nested).

Sharing Model
=============

The unit of sharing is a wavelet. In the first version of this protocol, all
participants on a wavelet have full access to modify the contents and
participant list of that wavelet.

Heterogeneous sharing within a wave is achieved by having differing participant
lists on wavelets within the wave. Currently, the two primary uses of this are
user-data and private replies.

User-data wavelets are used to store information which is private to an
individual user (that is, the user is the sole participant), such as
read/unread state.

A private reply is a wavelet whose participant list is a subset of that of the
parent wave.

Client-Server Protocol
######################

This section assumes an elementary understanding of the theory of Operational
Transformation (OT).

Operations
==========

Operations are mutations on wavelets. The state of a wavelet is entirely
defined by a sequence of operations on that wavelet.

Clients and servers exchange operations in order to communicate modifications
to a wavelet. Operations propagate through the system to all clients and
servers interested in that wavelet. They each apply the operation to their own
copy of the wavelet. In order for the wavelet state to be consistent throughout
the system, all communication participants (clients and servers) must apply
operations identically.

In a typical configuration, a wavelet it hosted by a master server - all
clients interested in a particular wavelet send operations to the hosting wave
server. The wave server acts as communication hub, storing operations and
echoing them to clients which are connected and 'interested' in that wavelet
(see "opening a wavelet" below). Wavelets may be federated, meaning that
wavelet servers can exchange operations about wavelets amongst themselves. For
more details see the Google Wave Federation Architecture whitepaper.  

Operation Sequencing
====================

Operational Transformation requires that the operations transmitted between
client and server be ordered. In Wave OT, the client never sends a "delta" (a
sequence of one or more operations) until the previous one has been
acknowledged by the server. The client is responsible for ordering the
operations that were received from the server before applying them to its local
copy of the wavelet copy. Operations are ordered according to a version number
provided by the server.

A client and server can verify that they are referring to the same wavelet
state by exchanging a version number and a "wavelet history hash". The latter
is a rolling hash over the sequence of operations between version zero and the
provided version number.

Opening a Wavelet
=================

A communication participant has a wavelet "open" if it is actively exchanging
operations pertaining to that wavelet. For the purposes of communication,
wavelets are grouped into a "wave-view", which is the set of wavelets on a wave
visible to a given user.  To open a wavelet, the client sends an Open Request
containing:

* Wave ID
* Wavelet ID

The server then responds with:
* A snapshot - the serialized state of the wavelet
* History hash at that version

Communicating changes to the Client
===================================

The server sends:

* Delta
* Version number
* History hash

Communicating changes to the Server
===================================

The client sends:

* Delta
* Version number

The server acknowledges the delta with:

* The version of the Wavelet after applying the delta
* History hash

The server can continue to send operations to the client while the client is
waiting for an acknowledgement. The client is responsible for transforming the
server operation and locally cached client operations (please refer to the
Google Wave Operational Transformation paper). The client sends the transformed
local operations to the server.

Recovery
========

When client-server communications fail, the client and server need to agree on
a common state of the wavelet upon reconnecting. The client reopens the
wavelet, sending a list of history hashes previously received from the server.

The client sends:

* Wave ID
* Wavelet ID
* List of history hashes known to the client

The server then responds with:

* Last known (by the server) history hash selected from the list of history hashes sent by the client (1)
* Most recent history hash on the Wavelet (2)
* A sequence of deltas

If the last known history hash (1) is the last history hash sent by the client,
and is equal to the most recent history hash (2), then the client and server
are in synch, and the client may resume receiving and sending deltas with no
further recovery.

If the last known history hash sent by the server does not match the last known
history hash sent by the client, or the server does not recognize any of the
client-provided hashes, the client and server have failed to agree on a common
state of the wavelet. The client must reload the wave at the server's current
state (the client-side state may be preserved for manual / prompted recovery of
data with the user).

The Google Wave Protocol contains optimizations to this recovery protocol that
reduce the number of cases requiring a complete state reset, but these are
beyond the scope of this document.

References
##########

David A. Nichols, Pavel Curtis, Michael Dixon, and John Lamping: `High-latency, low-bandwidth windowing in the Jupiter collaboration system`_, UIST '95: Proceedings of the 8th annual ACM symposium on User interface and software technology, pp.111-120. ACM, 1995.

.. _High-latency, low-bandwidth windowing in the Jupiter collaboration system: http://doi.acm.org/10.1145/215585.215706
