% Author:
% Date: 12/2/2010

add(0,Y,Y).
add(succ(X),Y,succ(Z)) :-
add(X,Y,Z).