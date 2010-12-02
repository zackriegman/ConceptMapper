% Author:
% Date: 12/2/2010

childOf(irina, natsha).
childOf(natsha, olga).
childOf(olga, katarina).
isIn(X, Y) :- childOf(X,Y).
isIn(X, Y) :- childOf(X,Z),isIn(Z,Y).