% Author:
% Date: 12/2/2010

child(martha,charlotte).
child(charlotte,caroline).
child(caroline,laura).
child(laura,rose).

descend(X,Y) :- child(X,Y).
descend(X,Y) :- child(X,Z),
descend(Z,Y).