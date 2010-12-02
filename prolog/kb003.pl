% Author:
% Date: 12/2/2010

parentOf(dan, zac).
parentOf(harold, dan).
%parentOf(Y, X) :- childOf(X, Y).

childOf(X, Y) :- parentOf(Y, X).

descendentOf(X, Y) :- childOf(X, Y).
descendentOf(X, Y) :- childOf(X, Z), descendentOf(Z, Y).
