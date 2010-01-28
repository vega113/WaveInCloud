######################################
Google Wave Operational Transformation
######################################

:Authors: 
  David Wang, 
  Alex Mah

:Version: 1.0 - May 2009

This whitepaper is part of a series. All of the whitepapers
can be found on `Google Wave Federation Protocol site`_.

.. _Google Wave Federation Protocol site: http://www.waveprotocol.org/whitepapers

Waves are hosted XML documents that allow seamless and low latency concurrent
modifications.  To provide this live experience, Wave uses Operational
Transformation (OT) as the theoretical framework of concurrency control.

Executive Summary
#################

Collaborative document editing means multiple editors being able to edit a
shared document at the same time.. Live and concurrent means being able to see
the changes another person is making, keystroke by keystroke.

Currently, there are already a number of products on the market that offer
collaborative document editing. Some offer live concurrent editing, such as
EtherPad and SubEthaEdit, but do not offer rich text. There are others that
offer rich text, such as Google Docs, but do not offer a seamless live
concurrent editing experience, as merge failures can occur.

Wave stands as a solution that offers both live concurrent editing and rich
text document support. 

The result is that Wave allows for a very engaging conversation where you can
see what the other person is typing, character by character much like how you
would converse in a cafe. This is very much like instant messaging except you
can see what the other person is typing, live. Wave also allows for a more
productive collaborative document editing experience, where people don't have
to worry about stepping on each others toes and still use common word processor
functionalities such as bold, italics, bullet points, and headings.

Wave is more than just rich text documents. In fact, Wave's core technology
allows live concurrent modifications of XML documents which can be used to
represent any structured content including system data that is shared between
clients and backend systems.

To achieve these goals, Wave uses a concurrency control system based on
Operational Transformation.

Introduction
############

Operational transformation (OT) is a theoretical framework of concurrency
control that has been continuously researched in the context of group editing
for more than 10 years. This document does not describe the basic theory of OT
and assumes the reader understands OT. The reader is encouraged to read the
documents in the reference section for background.

In short, Wave OT replicates the shared document at all sites and allows any
user to edit any part of the document at any time. Local editing operations are
executed without being delayed or blocked. Remote operations are transformed
before execution. The lock-free, non-blocking property of OT makes the local
response time insensitive to networking latencies. These properties of OT play
a big part in providing the Optimistic User Interface (UI) of Wave. Optimistic
UI means user actions are executed and displayed locally to the user
immediately without waiting for the server to respond.

The starting point for Wave was the paper "High-latency, low-bandwidth
windowing in the Jupiter collaboration system". Like the Jupiter system
described by the paper, Wave also implements a client and server based OT
system. The reader is again encouraged to read this paper for background.

In the Wave system, there are number of important objects. A Wave is a
collection of Wavelets. A Wavelet is a collection of documents. A document
consists of an XML document and some annotations. A Wavelet is where concurrent
modification takes place. A Wavelet is the object on which OT is applied.

This document will detail the extensions Wave made to the basic theory of OT
and how Wave operations support rich text documents. Most importantly, this
document details how we have designed Wave operations to transform large
numbers of operations efficiently and how they allow efficient look up into any
point of a Wavelet's history.

Wave Extensions to Operational Transformation 
##############################################

Clients wait for acknowledgement from server before sending more operations
===========================================================================

To recap, under the basic theory of OT, a client can send operations
sequentially to the server as quickly as it can. The server can do the same.
This means the client and server can traverse through the state space via
different OT paths to the same convergent state depending on when they receive
the other parties operations. See diagram below.

.. image:: img/ot-paths.png

When you have multiple clients connected to the server, every client and server
pair have their own state space. One short coming of this is the server needs
to carry a state space for every connected client which can be
memory-intensive. In addition, this complicates the server algorithm by
requiring it to convert clients' operations between state spaces.

Having a simple and efficient server is important in making Wave reliable and
scalable. With this goal, Wave OT modifies the basic theory of OT by requiring
the client to wait for acknowledgement from the server before sending more
operations. When a server acknowledges a client's operation, it means the
server has transformed the client's operation, applied it to the server's copy
of the Wavelet and broadcasted the transformed operation to all other connected
clients. Whilst the client is waiting for the acknowledgement, it caches
operations produced locally and sends them in bulk later.

With the addition of acknowledgements, a client can infer the server's OT path.
We call this inferred server path. By having this, the client can send
operations to the server that are always on the server's OT path. 

This has the important benefit that the server only needs to have a single
state space, which is the history of operations it has applied. When it
receives a client's operation, it only needs to transform the operation against
the operation history, apply the transformed operation, and then broadcast it.


.. image:: img/david_ot3.png


One trade off of this change is that a client will see chunks of operations
from another client in intervals of approximately one round trip time to the
other client. However, we believe the benefits obtained from the change make
this a worthwhile trade off.

Recovery
########

On top of OT, Wave also has the ability to recover from communication failure
and server crash. This is particularly important in a large scale system
environment.

Checksums
=========

OT assumes all the clients and the server are able to transform and execute the
operations in the same way. As an added guarantee that the resultant XML
documents are the same, Wave OT also communicates the checksums of the XML
documents with operations and acknowledgements. This allows a client to quickly
detect errors and recover from it by replacing corrupted contents with fresh
copies from the server. 

Wave Operations
###############


Wave operations consists of a document operation, for modifying XML documents
and other non document operations. Non document operations are for tasks such
as adding or removing a participant to a Wavelet. We'll focus on document
operations here as they are the most central to Wave.

It's worth noting that an XML document in Wave can be regarded as a single
document operation that can be applied to the empty document.

This section will also cover how Wave operations are particularly efficient
even in the face of a large number of transforms.

XML Document Support
====================

Wave uses a streaming interface for document operations. This is similar to an
XMLStreamWriter or a SAX handler. The document operation consists of a sequence
of ordered document mutations. The mutations are applied in sequence as you
traverse the document linearly. 

Designing document operations in this manner makes it easier to write
transformation function and composition function described later.

In Wave, every 16-bit Unicode code unit (as used in javascript, JSON, and Java
strings), start tag or end tag in an XML document is called an item. Gaps
between items are called positions. Position 0 is before the first item. A
document operation can contain mutations that reference positions. For example,
a "Skip" mutation specifies how many positions to skip ahead in the XML
document before applying the next mutation.

.. image:: img/doc-items.png

Wave document operations also support annotations. An annotation is some
meta-data associated with an item range, i.e. a start position and an end
position. This is particularly useful for describing text formatting and
spelling suggestions, as it does not unecessarily complicate the underlying XML
document format.

.. image:: img/annotations.png

Wave document operations consist of the following mutation components:

* skip
* insert characters
* insert element start
* insert element end
* insert anti-element start
* insert anti-element end
* delete characters
* delete element start
* delete element end
* delete anti-element start
* delete anti-element end
* set attributes
* update attributes
* commence annotation
* conclude annotation
 
The following is a more complex example document operation.::

  skip 3
  insert element start with tag "p" and no attributes
  insert characters "Hi there!"
  insert element end
  skip 5
  delete characters 4

From this, one could see how an entire XML document can be represented as a
single document operation. 

Transformation Function
=======================

Representing document operations using a stream interface has the benefit that
it makes processing operations in a linear fashion easy.

.. image:: img/transformation.png

The operation transformer works by taking two streaming operations as input,
simultaneously processing the two operations in a linear fashion, and
outputting two streaming operations. This stream-style processing ensures that
transforming a pair of very large operations is efficient.

Composition - Transforming large number of operations
=====================================================

The document operations have been engineered so that they can be composed
together and the composition of any two document operations that can be
composed together is itself a single document operation.

Furthermore, the composition algorithm processes operations as linear streams,
so the composition algorithm is efficient.

.. image:: img/composition.png


The composition operation has been designed to fulfil some requirements.

Firstly, the composition B∙A has the property that (B∙A)(d) = B(A(d))
for all documents on which A can be applied. This is the requirement from
the definition of composition.


The second requirement is that::

  transform(A,X) = (A',X') 

and::

  transform(B,X') = (B',X'')

implies::

  transform(B∙A,X) = (B'∙A', X'')

and that:: 

  transform(X,A) = (X',A')

and::

  transform(X',B) = (X'',B')

implies::

  transform(X,B∙A) = (X'',B∙A')

In traditional operational transformation frameworks, if the server and client
have gone far out of sync and have each accumulated a lot of concurrent
unacknowledged operations, transformation can be expensive.  If n is the number
of client operations the server has not yet acknowledged and m is the number of
server operations the client has not yet acknowledged, then nm transformations
are required to resolve the concurrency issue in a traditional operational
transformation framework.

Transforming many client operations with many server operations can be made
efficient if both composition is efficient and transformation of operations
resulting from composition is efficient.

We can design composition to be efficient enough that we can cut the
transformation running time to O(n log n + m log m), where n is the total size
of the client operations and m is the total size of the server operations.

References
##########

"Operational transformation". In Wikipedia, the free encyclopedia, May 28, 2009. http://en.wikipedia.org/wiki/Operational_transformation

David A. Nichols, Pavel Curtis, Michael Dixon, and John Lamping: `High-latency, low-bandwidth windowing in the Jupiter collaboration system`_, UIST '95: Proceedings of the 8th annual ACM symposium on User interface and software technology, pp.111-120. ACM, 1995.

.. _High-latency, low-bandwidth windowing in the Jupiter collaboration system: http://doi.acm.org/10.1145/215585.215706

