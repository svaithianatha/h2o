setwd(normalizePath(dirname(R.utils::commandArgs(asValues=TRUE)$"f")))
source('../findNSourceUtils.R')

test.principalcomp.golden <- function(H2Oserver) {
	
#Example from prcomp R doc

ausPath = system.file("extdata", "australia.csv", package="h2oRClient")
australia.hex = h2o.importFile(H2Oserver, path = ausPath)
australia.pca = h2o.prcomp(data = australia.hex, standardize = TRUE)
model<- print(australia.pca)
summary<- summary(australia.pca)

testEnd()
}

doTest("R Doc Principal Components Regression Ex", test.principalcomp.golden)