% Author:
% Date: 12/2/2010

loves(vincent,mia).
loves(marcellus,mia).

jealous(X,Y) :- loves(X,Z),loves(Y,Z).